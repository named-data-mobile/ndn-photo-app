package memphis.myapplication;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import timber.log.Timber;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

public class NSDHelper {
    private String serviceName = "npchat";
    private static final String SERVICE_TYPE = "_http._tcp.";

    private String m_userName;

    NsdManager m_nsdManager;
    ServerSocket m_serverSocket;
    NsdManager.RegistrationListener m_registrationListener;
    NsdServiceInfo m_serviceInfo;

    NsdManager.DiscoveryListener m_discoveryListener;
    private NsdManager.ResolveListener resolveListener;

    private Face m_face;
    private static Map<String, Integer> serviceNameToFaceId = new HashMap<String, Integer>();


    public NSDHelper(String userName, Context context, Face face) {
        m_userName = userName;
        m_face = face;

        try {
            m_serverSocket = new ServerSocket(0);
            m_serviceInfo = new NsdServiceInfo();
            m_serviceInfo.setServiceName(serviceName + "-" + userName);
            m_serviceInfo.setServiceType("_http._tcp.");
            m_serviceInfo.setAttribute("username", userName);
            m_serviceInfo.setPort(m_serverSocket.getLocalPort());

            m_nsdManager = (NsdManager) context.getSystemService(context.NSD_SERVICE);

            m_registrationListener = new MyRegistrationListener();

            m_nsdManager.registerService(m_serviceInfo, NsdManager.PROTOCOL_DNS_SD, m_registrationListener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unRegister() {
        m_nsdManager.unregisterService(m_registrationListener);
        m_nsdManager.stopServiceDiscovery(m_discoveryListener);
        for (int faceid : serviceNameToFaceId.values()) {
            try {
                Nfdc.destroyFace(m_face, faceid);
            } catch (ManagementException e) {
                e.printStackTrace();
            }
        }
    }

    // To be called only once from the application
    public void discoverServices() {
        m_discoveryListener = new MyDiscoveryListener();
        m_nsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, m_discoveryListener);
    }

    private class MyRegistrationListener implements NsdManager.RegistrationListener {

        @Override
        public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
            serviceName = NsdServiceInfo.getServiceName();
            Timber.d("Service name: " + serviceName);
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo arg0) {
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        }
    }

    private class MyDiscoveryListener implements NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        @Override
        public void onDiscoveryStarted(String regType) {
            Timber.d("Service discovery started");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found! Do something with it.
            Timber.d("Service discovery success" + service);
            if (!service.getServiceType().equals(SERVICE_TYPE)) {
                // Service type is the string containing the protocol and
                // transport layer for this service.
                Timber.d("Unknown Service Type: " + service.getServiceType());
            } else if (service.getServiceName().equals(serviceName)) {
                // The name of the service tells the user what they'd be
                // connecting to. It could be "Bob's Chat App".
                Timber.d("Same machine: " + serviceName);
            } else if (service.getServiceName().contains("npchat")){
                m_nsdManager.resolveService(service, new MyResolveListener());
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            Timber.e("service lost: " + service);
            // Destroy face (which will unregister routes)
            try {
                Integer faceid = serviceNameToFaceId.get(service.getServiceName());
                if (faceid != null) {
                    Nfdc.destroyFace(m_face, faceid);
                    serviceNameToFaceId.remove(service.getServiceName());
                }
            } catch (ManagementException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Timber.i("Discovery stopped: " + serviceType);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Timber.e("Discovery failed: Error code:" + errorCode);
            m_nsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Timber.e("Discovery failed: Error code:" + errorCode);
            m_nsdManager.stopServiceDiscovery(this);
        }
    }

    private class MyResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Called when the resolve fails. Use the error code to debug.
            Timber.e("Resolve failed: " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo sI) {
            Timber.e("Resolve Succeeded. " + sI);

            if (sI.getServiceName().equals(serviceName)) {
                Timber.d("Same IP.");
                return;
            }

            int faceid = 0;
            try {
                Timber.d("Created face: " + sI.getHost());
                // One slash is already in the sI.getHost(), need full canonical uri, other wise exception
                String uri = "udp4:/" + sI.getHost() + ":6363";
                faceid = Nfdc.createFace(m_face, uri);
                serviceNameToFaceId.put(sI.getServiceName(), faceid);

                //Timber.d("Testing: %s", new String(sI.getAttributes().get("username")));
                // This hashmap is empty for some reason. So currently parsing the service name to get the username
                String sIName = sI.getServiceName();
                Timber.d("NSD resolving: %s", sIName);
                Name npChatRoute = new Name("/npChat/" + sIName.substring(("npchat-").length()));
                Nfdc.register(m_face, faceid, npChatRoute, 0);
            } catch (ManagementException e) {
                e.printStackTrace();
            }
        }
    }
}
