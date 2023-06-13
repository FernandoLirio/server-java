import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@WebSocket
public class ChatWebSocket {
    private static Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    @OnWebSocketConnect
    public void onConnect(Session session) {
        sessions.add(session);
        System.out.println(sessions.size());
        broadcastUsers(getUsersCountJson());
        System.out.println("Nova conexão: " + session.getRemoteAddress().getAddress());
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        sessions.remove(session);
        System.out.println(sessions.size());
        broadcastUsers(getUsersCountJson());
        System.out.println("Conexão fechada: " + session.getRemoteAddress().getAddress());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        System.out.println("Mensagem recebida: " + message);
        broadcast(message);
    }

    private void broadcast(String message) {
        sessions.forEach(session -> {
            try {
                session.getRemote().sendString(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void broadcastUsers(String usersCount) {
        System.out.println("Enviando número: " + usersCount);
        sessions.forEach(session -> {
            try {
                session.getRemote().sendString(usersCount);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private String getUsersCountJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("usersCount", sessions.size());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public static class ChatWebSocketServlet extends WebSocketServlet {
        @Override
        public void configure(WebSocketServletFactory factory) {
            factory.register(ChatWebSocket.class);
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);

        WebSocketHandler webSocketHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory webSocketServletFactory) {
                webSocketServletFactory.register(ChatWebSocket.class);
            }
        };
        HandlerList handlerList = new HandlerList();
        handlerList.addHandler(webSocketHandler);

        server.setHandler(handlerList);
        server.start();
        server.join();
    }
}

