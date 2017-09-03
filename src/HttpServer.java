import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;  
import java.net.InetSocketAddress;  
import java.net.ServerSocket;  
import java.nio.ByteBuffer;  
import java.nio.CharBuffer;  
import java.nio.channels.*;  
import java.nio.charset.Charset;  
import java.nio.charset.CharsetDecoder;  
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;  
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;  

public class HttpServer 
{  
	//端口号
    private final static int PORT = 8888;  
    //Post上传默认的返回页面
    private final static String PostDefaultUrl="/pic.html";
	//服务器根路径
    private final static String HtmlBaseUrl="C:\\Users\\hp\\Desktop\\Server\\network";
	//返回的头，确保正确返回
    private final static String ReturnHttpHead="HTTP/1.1 200 OK\r\n\r\n"; 
    //定义全局channel
    private ServerSocketChannel ServerChannel;  
    //定义选择器
    private Selector ServerSelector;  
    //缓存用队列实现，里面的元素为map，标识文件名和具体的内容。
    static private Queue<Map<String,ArrayList<ByteBuffer>> > Queue= new LinkedList<Map<String,ArrayList<ByteBuffer>>>();


    /**
     * main函数
     * @param args
     */
	public static void main(String[] args) 
	{  
        try {  
            System.out.println("服务器端正在启动服务........");  
            HttpServer server = new HttpServer();  
            server.listen();  
        } 
        catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  
	
	/**
	 * 构造函数，初始化ServerSelector
	 * @throws IOException
	 */
    public HttpServer() throws IOException 
    {  
        ServerSelector = Selector.open();//打开选择器  
    }  
    
    /**
     * 监听函数
     * @throws IOException
     */
    public void listen() throws IOException 
    {  
        ServerChannel = ServerSocketChannel.open();  
        ServerSocket socket = ServerChannel.socket();  
        socket.bind(new InetSocketAddress(PORT));  
        ServerChannel.configureBlocking(false);  
        System.out.println("服务正在运行......");  
        while (true) 
        {  
            //非阻塞，没有连接
            SocketChannel client = ServerChannel.accept();  
            if (client != null) 
            {  
               //设置非阻塞  
                client.configureBlocking(false);  
                //将客户端channel注册到selector上  
                client.register(ServerSelector, SelectionKey.OP_READ);  
            }  
            if (ServerSelector.selectNow() > 0) 
            {  
                //客户端channel的键集合  
                Set<SelectionKey> keys = ServerSelector.selectedKeys();  
                Iterator<SelectionKey> it = keys.iterator();  
                while (it.hasNext()) 
                {  
                    SelectionKey key = it.next();  
                    if (key.isReadable()) 
                    {  
                        read(key);  
                        
                    } else if (key.isWritable())
                    {  
                        write(key);  
                    }  
                }  
      
            }  
        }  
    }  
    
  /**
   * 读获取到的http信息
   * @param key
   */
    private void read(SelectionKey key) 
    {  
        SocketChannel client = (SocketChannel) key.channel();  
        ByteArrayOutputStream Outputstream = new ByteArrayOutputStream();
        ByteBuffer imgbuf = ByteBuffer.allocate(40395);
        int imageBytes=-1;
        try {
			while ((imageBytes = client.read(imgbuf)) > 0)
			{
			    imgbuf.flip();
			    while(imgbuf.hasRemaining())
			    {
			        Outputstream.write(imgbuf.get());
			    }
			    imgbuf.compact();        
			}
		} catch (IOException e) {
			System.out.println("发生异常");
			e.printStackTrace();
		}
        byte[] buf=Outputstream.toByteArray();//获取内存缓冲中的数据
        
        System.out.println(Outputstream);
        
            HttpPaser httppaser = null;
			try {
				httppaser = new HttpPaser(Outputstream);
			} 
			catch (IOException e) 
			{
				//System.out.println("发生异常");
				e.printStackTrace();
				return ;
			}
			System.out.println(httppaser.method);
            if(httppaser.method.equals("POST"))
            {           	
            	 key.attach(PostDefaultUrl);//key参数携带参数
            	 System.out.println("用户名：   "+httppaser.username);
            	 System.out.println("密码：       "+httppaser.userpassword);
            	 Queue.clear();//清空缓存，以防出现的图片不一致
            }
            else if(httppaser.method.equals("GET"))
            {
            	key.attach(httppaser.uri);  //key参数携带参数
            }
        	key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);  
        	
    }  
  
    /**
     * 写文件到客户端
     * @param key
     * @throws IOException
     */
    private void write(SelectionKey key) throws IOException
    {  
        SocketChannel client = (SocketChannel) key.channel();  
        String uri = (String) key.attachment();			//取出read方法传递的信息。  
        String requestpath=HtmlBaseUrl+uri;
        ArrayList<ByteBuffer> arraylist=isBufferUri(uri);
        if(arraylist!=null)//如果是在缓存中则从缓存中得到结果
    	{
        	ByteBuffer block = ByteBuffer.wrap(ReturnHttpHead.getBytes());  
        	client.write(block);
        	 for(ByteBuffer bytebuffer: arraylist)
        	 {    
         		 client.write(bytebuffer);  
             }   
        	 client.close();  
        	 System.out.println("缓存文件数量为：   "+Queue.size());
    	}
        else//不在缓存中则查看是图片还是一般的文件，因为存储的方式不同
        {
        	//System.out.println("所请求内容不在缓存中");
        	String pic=uri.substring(uri.length()-3);
        	if(pic.equals("jpg")||pic.equals("png")||pic.equals("gif")||pic.equals("ico"))//图片文件的读取
            {
            		try {
                		File file = new File(requestpath);
                    	FileInputStream  fis = new FileInputStream(file);
                    	byte []sendBytes = new byte[65535];
                    	ByteBuffer block = ByteBuffer.wrap(ReturnHttpHead.getBytes());  
                    	client.write(block);
                    	
                    	//封装一个map
                	    Map<String,ArrayList<ByteBuffer>> map=new HashMap<String,ArrayList<ByteBuffer>>();
                	    ArrayList<ByteBuffer> commonFile=new ArrayList<ByteBuffer>();
                	    
                        while (( fis.read(sendBytes, 0, sendBytes.length)) > 0)
                        {
                        	ByteBuffer block2 = ByteBuffer.wrap(sendBytes);                       	
                        	commonFile.add(block2);     	    
                        	client.write(block2);
                        }
                        
                        map.put(uri, commonFile);       	    
                	    Queue.offer(map);
                	    if(Queue.size()>100)
                	    {
                	    	Queue.remove();
                	    }
                	}
                	catch(Exception e ) {
                		ByteBuffer block = ByteBuffer.wrap(("HTTP/1.1 404 Not Found"+ "\r\n").getBytes());  
                    	client.write(block);
                	} 
            		client.close();  
            }
        	else//一般文件的读取
        	{
        		ByteBuffer block = ByteBuffer.wrap(ReturnHttpHead.getBytes());  
            	client.write(block); 
            	
        		String contents=null;
        		try {
        			contents=new ReadFile(requestpath).readFile();
        			ByteBuffer content= ByteBuffer.wrap(contents.getBytes());  
            		client.write(content);  
            		
            		//封装一个map
            	    Map<String,ArrayList<ByteBuffer>> map=new HashMap<String,ArrayList<ByteBuffer>>();
            	    ArrayList<ByteBuffer> commonFile=new ArrayList<ByteBuffer>();
            	    commonFile.add(content);     	    
            	    map.put(uri, commonFile);       	    
            	    Queue.offer(map);
            	    if(Queue.size()>100)
            	    {
            	    	Queue.remove();
            	    }
        		}
            	catch(Exception e ) {
            		ByteBuffer block3 = ByteBuffer.wrap(("HTTP/1.1 404 Not Found" + "\r\n").getBytes());  
                	client.write(block3);
            	}   
        		client.close();  
        	}
        }

    }  

	public static String byteBufferToString(ByteBuffer buffer)
	{  
		CharBuffer charBuffer = null;  
		try {  
			Charset charset = Charset.forName("UTF-8");  
			CharsetDecoder decoder = charset.newDecoder();  
			charBuffer = decoder.decode(buffer);  
			buffer.flip();  
			return charBuffer.toString();  
		} 
		catch (Exception ex) {  
			ex.printStackTrace();  
		return null;  
		}  
	}  
	
    public ArrayList<ByteBuffer> isBufferUri(String requestpath) 
    {
    	 for (Map<String,ArrayList<ByteBuffer>>  map : Queue) 
    	 {   
    		 for (Map.Entry<String, ArrayList<ByteBuffer>> entry : map.entrySet()) 
    		 {   
    			// entry.getKey() + " = " + entry.getValue()
                 if(entry.getKey().equals(requestpath))
                 {
                	 //System.out.println("缓存文件数量为："+byteBufferToString(entry.getValue().get(0)));
                	 return entry.getValue();
                 }
                	 
    		 }   
    	 }   	
		return null;
	}


}  
  