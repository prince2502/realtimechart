package util;

import java.util.ArrayList;
import java.util.LinkedList;

import javafx.animation.AnimationTimer;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;


public class Timeline extends AnimationTimer{

	// height of the view if not changed
	private static int DEFAULT_HEIGHT = 80;

	// width of the view if not changed
	private static int DEFAULT_WIDTH = 600;

	// length of the timeline 
	private int interval;

	// width of each unit in pixels
	private int unit_size;

	// vertically mid of the graph from the top
	private int mid_position;

	// height of the view
	private int height;

	// width of the view
	private int width;

	// visuals
	private LinkedList<Line> graph_lines;
	private Pane graph;
	private HBox view;
	
	private Label median_label, top_label, bottom_label;
	
	// line color. Default is black, change by calling setStroke(Paint)
	private Paint line_color = Color.BLACK;

	// last calculated(during rescale) median of all the data
	private double current_median;
	
	// last calculated(during rescale) highest of all the data
	private double current_highest;
	
	// last calculated(during rescale) lowest of all the data
	private double current_lowest;

	// pixel to unit ratio, 2 means 2 unit values will fit in 1 pixel
	// last calculated scale 
	private double current_scale;

	// current data set
	private LinkedList<Double> current_values;

	// buffer used by externl entities to put data
	private ArrayList<Double> buffer;

	public Timeline(int interval){
		initialize(DEFAULT_HEIGHT, DEFAULT_WIDTH, interval);
	}

	public Timeline(int interval, int height, int width){
		initialize(height, width, interval);
	}

	private void initialize(int height, int width, int interval){
		this.interval = interval;

		this.height = height;
		this.width = width;

		this.unit_size = width / interval;
		this.mid_position = height / 2;

		graph_lines = new LinkedList<Line>();

		// first line in the graph
		Line first = makeLine(0, mid_position, unit_size, mid_position);
		graph_lines.add(first);

		current_values = new LinkedList<Double>();
		this.current_median = 0;
		this.current_highest = height / 2;
		this.current_lowest = -height / 2;
		this.current_scale = 1;
		
		// add initial lines to graph, depicting all values to be 0's, scale 1 and median 0
		for(int i = 1 ; i < interval ; i++){
			Line next = makeLine(graph_lines.getLast().getEndX(), mid_position, graph_lines.getLast().getEndX() + unit_size , mid_position);
			graph_lines.add(next);
			current_values.add(0.0);
		}

		graph = new Pane();
		graph.getChildren().addAll(graph_lines);
		graph.setMaxSize(width, height);
		graph.setMinSize(width, height);
		graph.setStyle("-fx-border-color: black; -fx-border-width: 0 0 0 1");
		
		// make legend
		VBox legend = new VBox();
		median_label = new Label(Integer.toString((int) current_median) + " uV");
		top_label = new Label(Integer.toString((int) current_highest)+ " uV");
		bottom_label = new Label(Integer.toString((int) current_lowest)+ " uV");
		
		Pane p1 = new Pane();
		Pane p2 = new Pane();
		
		legend.getChildren().addAll(top_label, p1, median_label, p2, bottom_label);
		VBox.setVgrow(p1, Priority.ALWAYS);
		VBox.setVgrow(p2, Priority.ALWAYS);
		
		view = new HBox();
		view.getChildren().addAll(legend, graph);
		
		buffer = new ArrayList<Double>();
	}

	public Pane getView(){
		return this.view;
	}

	/**
	 * add data to be updated at timeline
	 * do not add values without calling start first other wise buffer will overflow
	 * **/
	public void addValues(double[] values){
	
		synchronized(buffer){
			for(double value : values) buffer.add(value);
		}
	}

	/**
	 * add data to be updated at timeline
	 * do not add values without calling start first other wise buffer will overflow
	 * **/
	public void addValue(double value){

		synchronized(buffer){
			buffer.add(value);
		}

	}
	
	/**
	 * set stroke of the graph lines
	 * */
	public void setStroke(Paint color){
		this.line_color = color;

		for(Line line: this.graph_lines){
			line.setStroke(line_color);
		}
	}

	private Line makeLine(double startX, double startY, double endX, double endY){

		Line line = new Line(startX, startY, endX, endY);
		line.setStroke(line_color);
		line.setStrokeWidth(1);
		return line;
	}

	@Override
	public void handle(long now) {

		ArrayList<Double> local_buffer = new ArrayList<Double>();

		synchronized(buffer){
			local_buffer.addAll(buffer);
			buffer.clear();
		}

		if(local_buffer.isEmpty()) return;
		
		// rescale if need be
		reScale(local_buffer);

		// add new lines for all new values
		for (double raw: local_buffer){

			double value = (raw - current_median) / current_scale;

			Line next = makeLine(graph_lines.getLast().getEndX(), graph_lines.getLast().getEndY(), 
					graph_lines.getLast().getEndX() + unit_size , mid_position - value);

			graph_lines.add(next);
			graph.getChildren().add(next);

			for(Line line: this.graph_lines){

				line.setStartX(line.getStartX() - unit_size);
				line.setEndX(line.getEndX() - unit_size);

			}

			graph_lines.removeFirst();
			graph.getChildren().remove(0);
			this.current_values.remove(0);

			this.current_values.add(raw);	
		}

	}

	private void reScale(ArrayList<Double> buffer){

		double highest = this.current_highest ,  lowest = this.current_lowest;
		boolean reScaleNeeded = false;

		
		// if highest values in new values goes above top of the graph 
		// or if lowest value goes below the bottom then re scale is needed
		for (double raw: buffer){

			if (raw > highest){
				highest = raw;
				reScaleNeeded = true;
			}

			if (raw < lowest){
				lowest = raw;
				reScaleNeeded = true;
			}

		}

		// if all values (current + next) in the are too small then rescale is needed 
		if(!reScaleNeeded){
			
			highest = this.current_values.get(0);
			lowest = this.current_values.get(0);

			for(double raw: this.current_values){
				if(raw > highest) highest = raw;
				if(raw < lowest) lowest = raw;
			}
			
			for(double raw: buffer){
				if(raw > highest) highest = raw;
				if(raw < lowest) lowest = raw;
			}

			if( (highest - current_median) / current_scale < height * 0.3 ) reScaleNeeded = true;
			if( (lowest - current_median)  / current_scale > -height * 0.3 ) reScaleNeeded = true;

		}

		if(reScaleNeeded){

			double median = (highest + lowest) / 2;
			double scale = (highest - lowest ) / height; 

			graph_lines.clear();
			
			// add first line, use for the addition algorithm, will be removed later
			graph_lines.add(makeLine(0, mid_position, unit_size, mid_position));

			graph.getChildren().clear();
			
			for(double raw: this.current_values){

				double value = (raw - median) / scale;

				Line next = makeLine(graph_lines.getLast().getEndX(), graph_lines.getLast().getEndY(), 
						graph_lines.getLast().getEndX() + unit_size , mid_position - value);

				graph_lines.add(next);
				graph.getChildren().add(next);
			}

			graph_lines.removeFirst();

			this.current_highest = highest;
			this.current_lowest = lowest;
			this.current_median = median;
			this.current_scale = scale;
			
			this.median_label.setText(Integer.toString((int) current_median)+ " uV");
			this.top_label.setText(Integer.toString((int) current_highest)+ " uV");
			this.bottom_label.setText(Integer.toString((int) current_lowest)+ " uV");
		}
	}
}
