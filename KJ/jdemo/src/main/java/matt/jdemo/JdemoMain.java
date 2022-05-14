package matt.jdemo;

import javafx.application.Application;
import javafx.stage.Stage;

/*Error: JavaFX runtime components are missing, and are required to run this application*/

public class JdemoMain extends Application {
    public static void main(String[] args) {
        System.out.println("hello java");
        Application.launch(JdemoMain.class);
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("hello javafx");
    }
}
