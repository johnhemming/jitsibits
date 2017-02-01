package org.jitsi.videobridge.rest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ice4j.socket.DelegatingDatagramSocket;
import org.ice4j.socket.MultiplexedDatagramSocket;
import org.ice4j.socket.MultiplexingDatagramSocket;
import org.ice4j.socket.MultiplexingXXXSocketSupport;
import org.jitsi.impl.neomedia.MediaServiceImpl;
import org.jitsi.impl.neomedia.MediaStreamImpl;
import org.jitsi.service.libjitsi.LibJitsi;
import org.jitsi.service.neomedia.MediaService;
import org.jitsi.videobridge.Channel;
import org.jitsi.videobridge.Conference;
import org.jitsi.videobridge.Content;
import org.jitsi.videobridge.RtpChannel;
import org.jitsi.videobridge.RtpChannelDatagramFilter;
import org.jitsi.videobridge.VideoChannel;
import org.jitsi.videobridge.Videobridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.sf.fmj.media.rtp.RTCPSDESItem;
import net.sf.fmj.media.rtp.SSRCInfo;
import net.sf.fmj.media.rtp.SendSSRCInfo;

public class Status {

	public static boolean johnsBits=false; // controls my special logging.
	static DateFormat df = new SimpleDateFormat("HH:mm:ss");
	private static ConcurrentHashMap<SocketAddress,String> remoteAddresses=new ConcurrentHashMap<SocketAddress,String>();
	private static ConcurrentHashMap<SocketAddress,RtpChannel> remoteAddressesWithChannel=new ConcurrentHashMap<SocketAddress,RtpChannel>();
	
	public static ConcurrentHashMap<Long,String> cnames=new ConcurrentHashMap<Long,String>();
	public static ConcurrentHashMap<Long,Conference> conferenceToTell=new ConcurrentHashMap<Long,Conference>();
	public static ConcurrentHashMap<Long,String> endpoint=new ConcurrentHashMap<Long,String>();
	public static ConcurrentHashMap<Long,String> ids=new ConcurrentHashMap<Long,String>();
	public static ConcurrentHashMap<Long,String> channeltypes=new ConcurrentHashMap<Long,String>();
	
	private Status instance=null;	
	
	private static List<String> logItems=new ArrayList<String>();	

	public static void addLog(String s) {

	System.out.println(s);
	logItems.add(s);	
	}
	
	
	
	public static void dumpObject(Object o, HttpServletResponse response) throws Exception {
	
		
	
    Gson gsan = new GsonBuilder().setPrettyPrinting().create();
    String strang=gsan.toJson(o);
    response.getWriter().println("<BR>Dump of object<PRE>");
  //Gson gson = new Gson();
  response.getWriter().println(strang);
  response.getWriter().println("</PRE>");
	}


	public static void handle(String target, HttpServletRequest request, HttpServletResponse response, Videobridge videobridge) {
		try {

			
			MediaService ms=LibJitsi.getMediaService();
			MediaServiceImpl msi=(MediaServiceImpl)ms;
			
			response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("<h1>Status</h1>"+msi.getRtpCname());
			response.getWriter().println("number of conferences "+videobridge.getConferenceCount());
		
	        Conference[] cs = videobridge.getConferences();
	        if (cs != null && cs.length != 0)
	        {
	            for (Conference c : cs)
	            {
	            if (c==null) {
	            	response.getWriter().println("null conference");	
	            } else {
	            	response.getWriter().println("id:" +c.getID());
	            	if (c.isExpired()) {
		            	response.getWriter().println("expired");
	            	}
	            }
            	response.getWriter().println("<BR>");	
	            
            	Content[] contents=c.getContents();
	            for (Content content : contents) {
	            	response.getWriter().println(content.getName()+"channels:"+content.getChannelCount()+"<BR>");
	            	
	            Channel [] channels=content.getChannels();
	            for (Channel channel : channels) {
	            	
	            	String extra="";
	            	if (channel instanceof RtpChannel) {
	            		
	            		
	            		
	            		
	            	RtpChannel rc=(RtpChannel) channel;
	            	int [] rec=rc.getReceiveSSRCs();
	            	// looks like these are stored as they come rather than the last 8 signficant hex digits.
	            	
	            	
	            	extra=" sources localinitial="+rc.getInitialLocalSSRC()+" remote="+rc.getStream().getRemoteSourceID()+ " and =";
	            	extra+=Status.doSSRCArray(rec);

	            	try {
	            	SSRCInfo sinfo=rc.getStream().getStreamRTPManager().getSSRCCache().ourssrc;
	            	extra+="CN:"+sinfo.getCNAME();
	            	} catch (Exception e) {
	            	extra+="Error: "+e.getMessage();	
	            	}
	            	
	                MediaStreamImpl simpl=(MediaStreamImpl) rc.getStream();
	                
	            	Vector ss=simpl.getRTPManager().getSendStreams();
	            	extra+="Send Stream Count "+ss.size();
	            	for (Object s:ss) {
	            	if (s instanceof SendSSRCInfo) {
	            	SendSSRCInfo msi2=(SendSSRCInfo)s;
	            	extra+="CN:"+msi2.getCNAME()+ "ssrc: "+(msi2.getSSRC() & 0xffffffffl);
	            	} else {
	            	extra+=s.getClass().getSimpleName();	
	            	}
	            	}
	            	
	            	
	            	extra+=rc.seeStats();
	            	}
	
	            	extra+=" "+df.format(new Date(channel.getLastPayloadActivityTime()));
	            	extra+=" "+df.format(new Date(channel.getLastTransportActivityTime()));
	            	
	            	response.getWriter().println(channel.getClass().getName()+" Endpoint: "+channel.getEndpoint().getID()+ " Id:"+channel.getID()+" cb-id:"+channel.getChannelBundleId()+extra+"<BR>");
	            	
	            	if (channel instanceof VideoChannel) {
		            	VideoChannel vc=(VideoChannel) channel;
	            		
			        response.getWriter().print("Send to endpoints:");
		            List<String>ep=	vc.getLastNEndpoints();
		            for (String s : ep) {
		            response.getWriter().print(s+" ");	
		            }
		            response.getWriter().print("<BR>");
		            
	            	}
	            	
	            	
	            //dumpObject(channel,response);  goes recursive mad	
	            }
	            }
	        }

	        }
            response.getWriter().println("List cname source map<BR>");
			
	        Iterator it = cnames.entrySet().iterator();
	        while (it.hasNext()) {
	            Map.Entry pair = (Map.Entry)it.next();
	            response.getWriter().println(pair.getKey()+":"+pair.getValue()+"<BR>");
	            it.remove(); // avoids a ConcurrentModificationException
	        }
            response.getWriter().println("List sources<BR>");
			
	        it = remoteAddresses.entrySet().iterator();
	        while (it.hasNext()) {
	            Map.Entry pair = (Map.Entry)it.next();
	            response.getWriter().println(pair.getKey()+"<BR>");
	            it.remove(); // avoids a ConcurrentModificationException
	        }
	        
            response.getWriter().println("List sources and linked channels<BR>");
			
	        it = remoteAddressesWithChannel.entrySet().iterator();
	        while (it.hasNext()) {
	            Map.Entry pair = (Map.Entry)it.next();
	            response.getWriter().println(pair.getKey()+" "+((RtpChannel)pair.getValue()).getID()+"<BR>");
	            it.remove(); // avoids a ConcurrentModificationException
	        }
	        
            response.getWriter().println("List delegating sockets<BR>");
            for (DelegatingDatagramSocket d : datagramSockets) {
            	boolean typeFound=false;
            if (d instanceof MultiplexingDatagramSocket) {
            	typeFound=true;
            MultiplexingDatagramSocket m=(MultiplexingDatagramSocket)d;
            MultiplexingXXXSocketSupport<MultiplexedDatagramSocket> xxsup=m.multiplexingXXXSocketSupport;
            response.getWriter().println("XXX socket support" +xxsup.toString()+" socket count "+xxsup.sockets.size()+m.getPort()+" "+m.getLocalAddress()+"<BR>");
            
            for (MultiplexedDatagramSocket z:xxsup.sockets) {
            
                response.getWriter().println(reportMulti(z)+"<BR>");
            	
            	
            }
            }
            if (d instanceof MultiplexedDatagramSocket) {
            	typeFound=true;
            
                response.getWriter().println("as ind may repeat "+reportMulti((MultiplexedDatagramSocket)d)+"<BR>");
            }
            
            
            
            	
            if (typeFound==false){	
	        response.getWriter().println("unusual socket type "+d.getClass().getSimpleName());
            }
            }
	        
	        
	        
		
	        for (String a: logItems) {
	        response.getWriter().println(a+"<BR>");
	        }
	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			try {
			e.printStackTrace(response.getWriter());
			} catch (Exception e2) {
				e.printStackTrace(System.out);
				e2.printStackTrace(System.out);
			}
		}
	}

	public static String reportMulti(MultiplexedDatagramSocket z) {
		String x="";
		if (z.isBound()) {
		x+=" bound ";	
		} else {
		x+= " unbound ";	
		}
			
		if (z.isClosed()) {
		x+=" closed ";	
		} else {
		x+=" open ";	
		}
		
		
		if (z.getFilter() instanceof RtpChannelDatagramFilter) {
		RtpChannelDatagramFilter rtpf=(RtpChannelDatagramFilter) z.getFilter();
		
		RtpChannel rc=rtpf.channel;
		x+=rc.getClass().getSimpleName()+" ";
		int [] rpt=rc.getReceivePTs();
		for (int a:rpt) {
		x+=a+":";
		}
		if (rtpf.rtcp) {
		x+= " RTCP";	
		} else {
		x+= " RTP";	
		}
		
		}	
		
		try {
	return	z.getClass().getSimpleName()+" "+z.getTrafficClass()+" "+z.getPort()+" "+z.getLocalAddress()+" "+z.getFilter().getClass().getSimpleName()+x;
		} catch (Exception e) {
		return e.getMessage();	
		}
	}

	public static String doSSRCArray(int[] rec) {
		String extra="";
    	for (int r : rec) {
    		
    	extra+=(0xFFFFFFFFL & r);	
    	}
    	return extra;
	}



	public static void analysePacket(DatagramPacket p) {
		// this should track every packet received even ones that are not accepted.
		SocketAddress sa=p.getSocketAddress();
		String q=remoteAddresses.get(sa);
		if (q==null) remoteAddresses.put(sa, "abc");
		System.out.println("Status dg received  "+p.getLength());
		
	}



	public static void analysePacketAndRTPChannel(DatagramPacket p, RtpChannel rtpChannel) {
		SocketAddress sa=p.getSocketAddress();
		RtpChannel q=remoteAddressesWithChannel.get(sa);
		if (q==null) remoteAddressesWithChannel.put(sa,rtpChannel);
		
	}



	public static void map(long l, RTCPSDESItem item) {
		// TODO Auto-generated method stub

		String cname=item.getData();
		
		if (cnames.get(l)==null) {
			cnames.put(l, cname);
			Conference conference=conferenceToTell.get(l);
			if (conference==null) {
				System.out.println("Status: null conference to tell something wrong");
			} else {
				String msid=((MediaServiceImpl)LibJitsi.getMediaService()).getRtpCname();
				String endpointID=endpoint.get(l);
				String id=ids.get(l);
				String video=channeltypes.get(l);
			    conference.broadcastMessageOnDataChannels("{\"colibriClass\": \"john\", \"id\": \""+id+"\", \"channeltype\":\""+video+"\" ,\"ssrc\":\""+l+"\", \"endpoint\":\""+endpointID+"\", \"msid\":\""+msid+"\", \"cname\":\""+cname+"\"}");
			
			}
			
			
		}
		
		
	}



	public static void broadcastToConference(long ssrc, Conference conference, String endpointID, String id, String channeltype) {
		// TODO Auto-generated method stub
		System.out.println("BROADCAST TO CONFERENCE - new conference member "+endpointID+" "+ssrc);
		if (conferenceToTell.get(ssrc)==null) conferenceToTell.put(ssrc, conference);
		if (endpoint.get(ssrc)==null) endpoint.put(ssrc, endpointID);
		if (ids.get(ssrc)==null) ids.put(ssrc, endpointID);
		if (channeltypes.get(ssrc)==null) channeltypes.put(ssrc, channeltype);
		
	}
	public static void reportPosition() {

		reportPosition("",3);
	}	
	public static void reportPosition(String s) {

		reportPosition(s,3);
	}	

	static int stackCount=3;
	
	public static void reportPosition(String s,int stackStart) {
	
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		long threadNumber = Thread.currentThread().getId();

		String report=String.valueOf(threadNumber)+" ";
		for (int i=stackStart;i<ste.length & i<stackStart+stackCount;i++) {
			
		int spotty = ste[i].getClassName().lastIndexOf(".");
		String shortClass = ste[i].getClassName();		
		if (spotty > 0) {
			shortClass = shortClass.substring(spotty + 1);
		}
		
		report+=shortClass+"("+ste[i].getMethodName()+":"+ste[i].getLineNumber()+")";
		}
		if (s.length()>0) {
		System.out.println(s+" "+report);
		} else {
			System.out.println(report);
		}
		
	}


	static List<DelegatingDatagramSocket> datagramSockets=new ArrayList<DelegatingDatagramSocket>();

	public static void addDelegatingSocket(DelegatingDatagramSocket delegatingDatagramSocket) {
		// TODO this will cause memory leakage.
		datagramSockets.add(delegatingDatagramSocket);
		
		
		
	}
	
	
	
	
}
