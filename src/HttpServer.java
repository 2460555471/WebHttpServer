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
	//�˿ں�
    private final static int PORT = 8888;  
    //Post�ϴ�Ĭ�ϵķ���ҳ��
    private final static String PostDefaultUrl="/pic.html";
	//��������·��
    private final static String HtmlBaseUrl="C:\\Users\\hp\\Desktop\\Server\\network";
	//���ص�ͷ��ȷ����ȷ����
    private final static String ReturnHttpHead="HTTP/1.1 200 OK\r\n\r\n"; 
    //����ȫ��channel
    private ServerSocketChannel ServerChannel;  
    //����ѡ����
    private Selector ServerSelector;  
    //�����ö���ʵ�֣������Ԫ��Ϊmap����ʶ�ļ����;�������ݡ�
    static private Queue<Map<String,ArrayList<ByteBuffer>> > Queue= new LinkedList<Map<String,ArrayList<ByteBuffer>>>();


    /**
     * main����
     * @param args
     */
	public static void main(String[] args) 
	{  
        try {  
            System.out.println("��������������������........");  
            HttpServer server = new HttpServer();  
            server.listen();  
        } 
        catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  
	
	/**
	 * ���캯������ʼ��ServerSelector
	 * @throws IOException
	 */
    public HttpServer() throws IOException 
    {  
        ServerSelector = Selector.open();//��ѡ����  
    }  
    
    /**
     * ��������
     * @throws IOException
     */
    public void listen() throws IOException 
    {  
        ServerChannel = ServerSocketChannel.open();  
        ServerSocket socket = ServerChannel.socket();  
        socket.bind(new InetSocketAddress(PORT));  
        ServerChannel.configureBlocking(false);  
        System.out.println("������������......");  
        while (true) 
        {  
            //��������û������
            SocketChannel client = ServerChannel.accept();  
            if (client != null) 
            {  
               //���÷�����  
                client.configureBlocking(false);  
                //���ͻ���channelע�ᵽselector��  
                client.register(ServerSelector, SelectionKey.OP_READ);  
            }  
            if (ServerSelector.selectNow() > 0) 
            {  
                //�ͻ���channel�ļ�����  
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
   * ����ȡ����http��Ϣ
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
			System.out.println("�����쳣");
			e.printStackTrace();
		}
        byte[] buf=Outputstream.toByteArray();//��ȡ�ڴ滺���е�����
        
        System.out.println(Outputstream);
        
            HttpPaser httppaser = null;
			try {
				httppaser = new HttpPaser(Outputstream);
			} 
			catch (IOException e) 
			{
				//System.out.println("�����쳣");
				e.printStackTrace();
				return ;
			}
			System.out.println(httppaser.method);
            if(httppaser.method.equals("POST"))
            {           	
            	 key.attach(PostDefaultUrl);//key����Я������
            	 System.out.println("�û�����   "+httppaser.username);
            	 System.out.println("���룺       "+httppaser.userpassword);
            	 Queue.clear();//��ջ��棬�Է����ֵ�ͼƬ��һ��
            }
            else if(httppaser.method.equals("GET"))
            {
            	key.attach(httppaser.uri);  //key����Я������
            }
        	key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);  
        	
    }  
  
    /**
     * д�ļ����ͻ���
     * @param key
     * @throws IOException
     */
    private void write(SelectionKey key) throws IOException
    {  
        SocketChannel client = (SocketChannel) key.channel();  
        String uri = (String) key.attachment();			//ȡ��read�������ݵ���Ϣ��  
        String requestpath=HtmlBaseUrl+uri;
        ArrayList<ByteBuffer> arraylist=isBufferUri(uri);
        if(arraylist!=null)//������ڻ�������ӻ����еõ����
    	{
        	ByteBuffer block = ByteBuffer.wrap(ReturnHttpHead.getBytes());  
        	client.write(block);
        	 for(ByteBuffer bytebuffer: arraylist)
        	 {    
         		 client.write(bytebuffer);  
             }   
        	 client.close();  
        	 System.out.println("�����ļ�����Ϊ��   "+Queue.size());
    	}
        else//���ڻ�������鿴��ͼƬ����һ����ļ�����Ϊ�洢�ķ�ʽ��ͬ
        {
        	//System.out.println("���������ݲ��ڻ�����");
        	String pic=uri.substring(uri.length()-3);
        	if(pic.equals("jpg")||pic.equals("png")||pic.equals("gif")||pic.equals("ico"))//ͼƬ�ļ��Ķ�ȡ
            {
            		try {
                		File file = new File(requestpath);
                    	FileInputStream  fis = new FileInputStream(file);
                    	byte []sendBytes = new byte[65535];
                    	ByteBuffer block = ByteBuffer.wrap(ReturnHttpHead.getBytes());  
                    	client.write(block);
                    	
                    	//��װһ��map
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
        	else//һ���ļ��Ķ�ȡ
        	{
        		ByteBuffer block = ByteBuffer.wrap(ReturnHttpHead.getBytes());  
            	client.write(block); 
            	
        		String contents=null;
        		try {
        			contents=new ReadFile(requestpath).readFile();
        			ByteBuffer content= ByteBuffer.wrap(contents.getBytes());  
            		client.write(content);  
            		
            		//��װһ��map
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
                	 //System.out.println("�����ļ�����Ϊ��"+byteBufferToString(entry.getValue().get(0)));
                	 return entry.getValue();
                 }
                	 
    		 }   
    	 }   	
		return null;
	}


}  
  