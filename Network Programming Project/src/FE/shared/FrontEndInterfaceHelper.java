package shared;


/**
* shared/FrontEndInterfaceHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from IDL.idl
* Wednesday, November 18, 2020 3:14:18 PM EST
*/

abstract public class FrontEndInterfaceHelper
{
  private static String  _id = "IDL:shared/FrontEndInterface:1.0";

  public static void insert (org.omg.CORBA.Any a, shared.FrontEndInterface that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static shared.FrontEndInterface extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (shared.FrontEndInterfaceHelper.id (), "FrontEndInterface");
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static shared.FrontEndInterface read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_FrontEndInterfaceStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, shared.FrontEndInterface value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static shared.FrontEndInterface narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof shared.FrontEndInterface)
      return (shared.FrontEndInterface)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      shared._FrontEndInterfaceStub stub = new shared._FrontEndInterfaceStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

  public static shared.FrontEndInterface unchecked_narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof shared.FrontEndInterface)
      return (shared.FrontEndInterface)obj;
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      shared._FrontEndInterfaceStub stub = new shared._FrontEndInterfaceStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

}