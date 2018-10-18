package application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import util.Timeline;


/**
 * Dummy application to test usage of Timeline
 * **/

public class Driver extends Application{

	
	private ArrayList<Timeline> timelines = new ArrayList<Timeline>(14);

	Paint[] strokes = new Paint[]{
			Color.BLUEVIOLET , Color.FORESTGREEN, Color.DARKORANGE, Color.DARKGREEN,
			Color.RED, Color.LIGHTSEAGREEN, Color.ORCHID, 
			Color.BLUEVIOLET , Color.FORESTGREEN , Color.DARKORANGE, Color.DARKGREEN,
			Color.RED, Color.LIGHTSEAGREEN, Color.ORCHID
	};

	String[] node_names = new String[]{
			"G1", "G2", "G3", "G4", "G5", "G6", "G7", "G8", "G9", "G10", "G11", "G12", "G13", "G14"
	};


	@Override
	public void start(Stage stage) {

		GridPane graph_holder = new GridPane();
		graph_holder.setVgap(15);
		graph_holder.setHgap(15);
		graph_holder.setPadding(new Insets(20));

		for(int i = 0 ; i < 14 ; i ++){

			Timeline timeline = new Timeline(280, 80 , 560);
			timeline.setStroke(strokes[i]);
			timelines.add(timeline);

			// always call this before adding data to timeline
			timeline.start();
			
			// add each timeline view to the grid
			graph_holder.add(timeline.getView(), (i%2)*2 + 1, i/2);
			
			// add name for all the timelines
			graph_holder.add(new Label(node_names[i]), (i%2)* 2 , i/2);

		}
		
		// just making sure someone accidently don't make the window smaller
		graph_holder.setMinSize(1366, 768);
		
		Scene scene = new Scene(graph_holder);		
		stage.setScene(scene);

		stage.show();
		stage.setMaximized(true);
		

		// use an external thread to enter the dummy data
		(new Thread(()->{
			
			for (int j = 0; j < 1000000 ; j++){ // add data to all graphs for some long time(can be forever)
				for (int i = 0 ; i < 14 ; i++) {
					
					Random random = new Random();
					
					double val = random.nextDouble();
					if (i == 5) val *= j;      // G6 is an increasing graph, for fun
					
					timelines.get(i).addValue(val); 
				}
				
				// just wait for a while, other wise it is too overwhenling for the UI
				// test your self how fast it can go .. 
				try {
					Thread.sleep(100);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		})).start();
		
    }

	public static void main(String[] args) throws IOException {
		launch(args);
	}

}
