package com.culturalcenter.server;

import com.culturalcenter.server.controllers.AuthController;
import com.culturalcenter.server.controllers.UserController;
import com.culturalcenter.server.controllers.CulturalCenterController;
import com.culturalcenter.server.controllers.RoomController;
import com.culturalcenter.server.network.Request;
import com.culturalcenter.server.network.Response;
import com.culturalcenter.server.controllers.BookingController;
import com.culturalcenter.server.controllers.ReportController;
import com.culturalcenter.server.controllers.ReviewController;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientThread implements Runnable {
    private final Socket socket;
    private final AuthController authController;
    private final UserController userController;
    private final CulturalCenterController culturalCenterController;
    private final RoomController roomController;
    private final BookingController bookingController;
    private final ReportController reportController;
    private final ReviewController reviewController;


    public ClientThread(Socket socket) {
        this.socket = socket;
        this.authController = new AuthController();
        this.userController = new UserController();
        this.culturalCenterController = new CulturalCenterController();
        this.roomController = new RoomController();
        this.bookingController = new BookingController();
        this.reportController = new ReportController();
        this.reviewController = new ReviewController();
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            System.out.println("Обработка клиента...");
            Request request = (Request) in.readObject(); //чтение запроса от клиента
            System.out.println("Получен запрос: " + request.getOperation());
            Response response = handleRequest(request);
            out.writeObject(response); //Отправка ответа клиенту
            System.out.println("Отправлен ответ: " + response.isSuccess());

        } catch (Exception e) {
            System.err.println("Ошибка при обработке клиента: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Response handleRequest(Request request) { //Маршрутизация запросов
        String operation = request.getOperation();
        String data = request.getData();

        switch (operation) {
            case "LOGIN":
                return authController.login(data);
            case "REGISTER":
                return authController.register(data);
            case "GET_USER":
                return authController.getUser(data);
            case "UPDATE_USER":
                return authController.updateUser(data);
            case "GET_USER_BY_ID":
                return authController.getUserById(data);
            case "GET_ALL_USERS":
                return userController.getAllUsers();
            case "CREATE_USER":
                return userController.createUser(data);
            case "UPDATE_USER_BY_ADMIN":
                return userController.updateUserByAdmin(data);
            case "DELETE_USER":
                return userController.deleteUser(data);
            case "BLOCK_USER":
                return userController.blockUser(data);
            case "UNBLOCK_USER":
                return userController.unblockUser(data);
            case "ASSIGN_DIRECTOR":
                return userController.assignDirector(data);
            case "GET_ALL_CENTERS":
                return culturalCenterController.getAllCenters();
            case "GET_CENTER_BY_ID":
                return culturalCenterController.getCenterById(data);
            case "CREATE_CENTER":
                return culturalCenterController.createCenter(data);
            case "UPDATE_CENTER":
                return culturalCenterController.updateCenter(data);
            case "DELETE_CENTER":
                return culturalCenterController.deleteCenter(data);
            case "GET_CENTERS_FOR_DROPDOWN":
                return culturalCenterController.getCentersForDropdown();
            case "GET_ROOMS_BY_CENTER":
                return roomController.getRoomsByCenterId(data);
            case "CREATE_ROOM":
                return roomController.createRoom(data);
            case "UPDATE_ROOM":
                return roomController.updateRoom(data);
            case "DELETE_ROOM":
                return roomController.deleteRoom(data);
            case "CHECK_ROOM_HAS_BOOKINGS":
                return roomController.checkRoomHasBookings(data);
            case "GET_BOOKINGS_BY_CENTER":
                return bookingController.getBookingsByCenter(data);
            case "APPROVE_BOOKING":
                return bookingController.approveBooking(data);
            case "REJECT_BOOKING":
                return bookingController.rejectBooking(data);
            case "GET_MY_CENTER":
                return culturalCenterController.getMyCenter(data);
            case "GET_MY_ROOMS":
                return roomController.getRoomsByCenterId(data);
            case "CLOSE_ROOM":
                return roomController.closeRoom(data);
            case "OPEN_ROOM":
                return roomController.openRoom(data);
            case "GENERATE_REPORT":
                return reportController.generateReport(data);
            case "CREATE_BOOKING":
                return bookingController.createBooking(data);
            case "GET_MY_BOOKINGS":
                return bookingController.getMyBookings(data);
            case "CANCEL_BOOKING":
                return bookingController.cancelBooking(data);
            case "CREATE_REVIEW":
                return reviewController.createReview(data);
            case "GET_UNREAD_NOTIFICATIONS":
                return bookingController.getUnreadNotifications(data);
            case "UPDATE_COMPLETED_BOOKINGS":
                return bookingController.updateCompletedBookings();
            default:
                return new Response(false, "Неизвестная операция: " + operation, null);
        }
    }
}