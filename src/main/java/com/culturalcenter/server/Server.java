package com.culturalcenter.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.InputStream;
import java.util.Properties;

public class Server {
    private static int PORT;

    static {
        try (InputStream input = Server.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties props = new Properties();
            props.load(input);
            PORT = Integer.parseInt(props.getProperty("server.port"));
        } catch (Exception e) {
            PORT = 8080; // значение по умолчанию
        }
    }
    private static final ExecutorService threadPool = Executors.newCachedThreadPool(); // ← пул потоков

    public static void main(String[] args) {
        System.out.println("ЗАПУСК СЕРВЕРА...");
        System.out.println("Порт: " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер успешно запущен!");
            System.out.println("Ожидание подключений...");

            while (true) {
                Socket clientSocket = serverSocket.accept(); //этот метод заставляет программу ждать, пока какой-нибудь клиент не подключится
                System.out.println("Новый клиент подключился: " + clientSocket.getInetAddress());
                threadPool.submit(new ClientThread(clientSocket)); //отдаём задачу в пул
            }

        } catch (Exception e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }
}