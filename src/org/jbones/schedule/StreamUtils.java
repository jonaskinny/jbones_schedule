package org.jbones.schedule;

import org.jbones.core.*;
import org.jbones.core.log.*;

import java.io.*;


/**
transfers bytes from an InputStream to an OutputStream; like a pipe or a patch cable.
Util functions for dealing with byte streams; where you want to serve bytes.
Read bytes from something (like a file), and send to something else
(like a HttpServletResponse ect).
Used in a lot of places - DataStreamStoreServer, ContentMgmtSys, etc.
*/
public class StreamUtils
{

   /**
      does a buffered transfer of 'bytes' number of bytes
      in the inputstream to the outputstream.
   **/
   public static void transfer(InputStream in, OutputStream out, long bytes)
      throws Exception
   {
      byte[] bs = new byte[32768];
      int bts = 0;
      while(bytes>0)
      {
         if(bytes<bs.length)
         {
            bts = (int)bytes;
            bytes = 0;
         }
         else
         {
            bts = bs.length;
            bytes-=bs.length;
         }
         in.read(bs,0,bts);
         out.write(bs,0,bts);
      }
      out.flush();
   }
   
   /**
      very slow byte by byte transfer until -1 is reached.
   **/
   public static void transferUntilNegativeOne(InputStream in, OutputStream out)
      throws Exception
   {
      int oneChar;
      while ((oneChar=in.read()) != -1)
         out.write(oneChar);
   }
   
   
   /**
      transfers ALL bytes from in to out.  the end of input is specified by
      a -1 byte.
   **/
   public static void transfer(InputStream in, OutputStream out)
      throws Exception
   {
      BufferedInputStream bin = new BufferedInputStream(in, 32768);
      byte[] bs = new byte[32768];
      int actual = 0;
      while(actual!=-1)
      {
         actual = in.read(bs,0,bs.length);
         if(actual==-1) continue;
         out.write(bs,0,actual);
      }
      out.flush();
   }
   
   
   public static void transfer(byte[] inbytes, OutputStream out)
      throws Exception
   {
      transfer(inbytes, out, inbytes.length);
   }   
   
   public static void transfer(byte[] inbytes, OutputStream out, long bytes)
      throws Exception
   {
      InputStream in = null;
      try{
         in = new ByteArrayInputStream(inbytes);
         transfer(in,out,bytes);
         in.close();
      }catch(Exception e){
         if(in!=null)
            try{in.close();}catch(Exception ex){
               Log.getLog(Log.ERR).log(
               "[org.jbones.schedule.StreamUtils.transfer(byte[],OutputStream,bytes):inclose:"+ex.toString()+"]");}
         throw e;
      }
   }
   
   
   public static void closeStream(InputStream inputStream)
   {
      if (inputStream==null ) return;
      try {
         inputStream.close();
      } catch ( Exception e){
         Log.getLog(Log.ERR).log(e);
      }
   }
   
   public static void closeStream(OutputStream outputStream)
   {
      if (outputStream==null ) return;
      try {
         outputStream.flush();
         outputStream.close();
      } catch ( Exception e){
         Log.getLog(Log.ERR).log(e);
      }
   }
}
