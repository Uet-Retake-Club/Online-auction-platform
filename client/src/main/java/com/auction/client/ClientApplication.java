package com.auction.client; 

import com.auction.client.services.NetworkClientService;
import javafx.application.Application;
import javafx.stage.Stage;
import com.auction.client.utils.SceneNavigator;

public class ClientApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        // MỞ KẾT NỐI TỚI SERVER NGAY KHI BẬT APP
        NetworkClientService.getInstance().connect("localhost", 8080); // Cổng 8080

        // CẤU HÌNH GIAO DIỆN JAVAFX
        SceneNavigator.init(primaryStage);
        primaryStage.setTitle("AuctionHub");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
        primaryStage.show();
    }

    // NGẮT KẾT NỐI AN TOÀN KHI TẮT APP (Tránh kẹt Port)
    @Override
    public void stop() throws Exception {
        NetworkClientService.getInstance().disconnect();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
//Tun