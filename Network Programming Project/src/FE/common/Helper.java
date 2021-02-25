package common;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omg.CORBA.*;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import implementation.FrontEnd;
import shared.FrontEndInterface;
import shared.FrontEndInterfaceHelper;

public class Helper {

	
	public static FrontEndInterface getInterface(ORB orb, String region) throws NotFound, CannotProceed, InvalidName, org.omg.CORBA.ORBPackage.InvalidName {
		org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");  	 
    	NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
    	FrontEndInterface server = FrontEndInterfaceHelper.narrow(ncRef.resolve_str(region));
    	
    	return server;
	}
	
	public String ExtractRegion(String id) {
		return id.substring(0,2);
	}
	
	public static boolean ValidateItemId(String id) {
		Pattern p = Pattern.compile("(QC|ON|BC)([0-9]){4}");
   	 	Matcher m = p.matcher(id);
   	 	boolean b = m.matches();
   	 	
   	 	return b;
	}
	
	
	
	public static Logger initiateLogger(String userId) throws IOException {
		Files.createDirectories(Paths.get("Logs/Users"));
		
		Logger logger = Logger.getLogger("Logs/Users/" + userId + ".log");
		FileHandler fileHandler;
		
		try
		{
			fileHandler = new FileHandler("Logs/Users/" + userId + ".log");
			
			logger.setUseParentHandlers(false);
			logger.addHandler(fileHandler);
			
			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);
		}
		
		catch (IOException e)
		{
			System.err.println("IO Exception " + e);
			e.printStackTrace();
		}
		
		System.out.println("Server can successfully log.");
		
		return logger;
	}
	
	
	
	public static void initializeORB(String[] args, String uuid)  {
		new Thread(new Runnable() {
            @Override
            public void run() {
            	try {
            		
            		ORB orb = ORB.init(args, null);
    				
    				POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    				rootPOA.the_POAManager().activate();
    				
    				FrontEnd frontEnd = new FrontEnd(orb, uuid);
					org.omg.CORBA.Object ref = rootPOA.servant_to_reference(frontEnd);
					FrontEndInterface href = FrontEndInterfaceHelper.narrow(ref);
					
					//9. Bind the Object Reference in Naming
					org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
					
					NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
					
					NameComponent path[] = ncRef.to_name(uuid);
					ncRef.rebind(path, href);
    		    	orb.run();
            	}
            	catch(Exception ex) {
            		
            	}
            }
        }).start();
	  
		
		System.out.println("FrontEnd created");
	}
}
