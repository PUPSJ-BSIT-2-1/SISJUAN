package com.example.pupsis_main_dashboard.controllers;

import com.example.pupsis_main_dashboard.models.Schedule;
import com.example.pupsis_main_dashboard.utilities.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;


public class RoomAssignmentController {

    @FXML
    private TableView<Schedule> studentTable;
    @FXML
    private TableColumn<Schedule, String> subjCodeCell;
    @FXML
    private TableColumn<Schedule, String> subjDescriptionCell;
    @FXML
    private TableColumn<Schedule, String> hourCell;
    @FXML
    private TableColumn<Schedule, String> unitsCell;
    @FXML
    private TableColumn<Schedule, String> scheduleCell;
    @FXML
    private TableColumn<Schedule, String> roomCell;
    @FXML
    private VBox root;
    @FXML
    private Label semester;
    @FXML
    private ImageView lecture;
    @FXML
    private ImageView laboratory;
    @FXML
    private ImageView time;

    private final ObservableList<Schedule> scheduleList = FXCollections.observableArrayList();

    private static final Logger logger = LoggerFactory.getLogger(RoomAssignmentController.class);

}


