package com.culturalcenter.client.network;

import com.culturalcenter.server.network.Request;
import com.culturalcenter.server.network.Response;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NetworkClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    public static Response sendRequest(String operation, String data) { //Клиент отправляет запрос на сервер
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);   // ← открываем сокет
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            Request request = new Request(operation, data);
            out.writeObject(request);  // ← отправляем запрос

            return (Response) in.readObject(); // ← получаем ответ

        } catch (Exception e) {
            System.err.println("Ошибка соединения с сервером: " + e.getMessage());
            return new Response(false, "Ошибка соединения с сервером. Убедитесь, что сервер запущен.", null);
        }
    }
}