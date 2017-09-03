import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HttpPaser 
{
		public String method;
		public String uri;
		public String boundary;
		public String username;
		public String userpassword;
		public final static String PicUrl="C:\\Users\\hp\\Desktop\\Server\\network\\pic\\pic.jpg";
		
		public  HttpPaser(ByteArrayOutputStream stream) throws IOException
		{
			//writeLog(stream.toString());
			parser(stream);
		}
		public static void writeLog(String str)
	    {
	        try
	        {
	        String path="C:\\Users\\hp\\Desktop\\Server\\1.txt"; // 输出文件路径  
	        File file=new File(path);
	        if(!file.exists())
	            file.createNewFile();     
	        FileOutputStream out=new FileOutputStream(file,true); //如果追加方式用true        
	        StringBuffer sb=new StringBuffer();
	        sb.append(str+"\n");
	        out.write(sb.toString().getBytes("utf-8"));//注意需要转换对应的字符集
	        out.close();
	        }
	        catch(IOException ex)
	        {
	            System.out.println(ex.getStackTrace());
	        }
	    }

		public  void parser(ByteArrayOutputStream stream) throws IOException 
		{  

			String s=stream.toString();
		    if (s.startsWith("GET")) 
		    {
		    	method = "GET";  
		        int index = s.indexOf("HTTP");  
		        uri = s.substring(3 + 1, index - 1);
		    }
		    else if (s.startsWith("POST"))
		    {  
		        method = "POST";  
		        int type_position=s.indexOf("Content-Type: multipart/form-data; boundary");
		        int type_position_start=s.indexOf( "=",type_position)+1;
		        int type_position_end=s.indexOf("Accept: ",type_position)-2;
		        boundary=s.substring(type_position_start, type_position_end);
		        
		        int nameindex=s.indexOf("name=\"name\"")+15;
		        int nameindex_end=s.indexOf(boundary,nameindex)-3;
		        username=s.substring(nameindex, nameindex_end).trim();
		             
		        int passwordindex=s.indexOf("name=\"password\"")+17;
		        int passwordindex_end=s.indexOf(boundary,passwordindex)-3;
		        userpassword=s.substring(passwordindex, passwordindex_end).trim();
		        
		        int image=s.indexOf("Content-Type:",passwordindex_end);
		        int image_start=s.indexOf("\n",image)+3;
		        
		    	byte[] buf=stream.toByteArray();  	
		    	int length=buf.length-boundary.length()-2;
		    	
		    	byte[] buf2=new byte[length-image_start+1];
		    	int k=0;
		    	for(int i=image_start;i<length;i++,k++)
		    	{
		    		buf2[k]=buf[i];
		    	}
		    	
		    	//DeleteFile(new File(PicUrl));
		    	
		        OutputStream out = new FileOutputStream(PicUrl);
		        InputStream is = new ByteArrayInputStream(buf2);
		        byte[] buff = new byte[1024];
		        int len = 0;
		        while((len=is.read(buff))!=-1)
		        {
		            out.write(buff, 0, len);
		        }
		        is.close();
		        out.close();
		    }     
		} 
		private static void DeleteFile(File _directory) 
		{  
		    if(_directory.isFile()) 
		    {  
		        _directory.delete();  
		        return;  
		    }  
		    File[] fileArr = _directory.listFiles();  
		    for(File file:fileArr) {  
		       DeleteFile(file);  
		    }  
		    _directory.delete();  
		}
		  
		  



}