import java.io.*;  
  
public class ReadFile 
{  
	public String contents;
	public String path;
	
	public ReadFile(String pa)
	{
		this.path=pa;
	}
    public String  readFile() 
    {  
        try {  
            FileInputStream in = new FileInputStream(path); // 读取文件路径  
            byte bs[] = new byte[in.available()];  
            in.read(bs);  
            contents=new String(bs);
            in.close();  
        } 
        catch (Exception e) 
        {  
            e.printStackTrace();  
        }  
        return contents;
    }  
 
}  