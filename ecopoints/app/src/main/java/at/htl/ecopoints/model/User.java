package at.htl.ecopoints.model;

import java.util.LinkedList;
import java.util.List;

import at.htl.ecopoints.R;

public class User {
    private Long id;
    private String userName;
    private String password;
    private Double ecoPoints;

    private List<List<CardContent>> detailRankingCardContentList;

    public User() {
    }

    public User(Long id, String username, String password, Double ecoPoints) {
        this.id = id;
        this.userName = username;
        this.password = password;
        this.ecoPoints = ecoPoints;



        this.detailRankingCardContentList = new LinkedList<>(){
            {
                add(new LinkedList<>(){
                    {
                        add(new CardContent(ecoPoints.toString(), "Eco-Points", R.drawable.ranking_category_ecopoints));
                        add(new CardContent("14,2 kg", "CO₂-Consumption", R.drawable.ranking_category_co2));
                    }
                });
                add(new LinkedList<>(){
                    {
                        add(new CardContent("5", "Trips", R.drawable.ranking_category_trips));
                        add(new CardContent("100 km", "Distance", R.drawable.ranking_category_distance));
                    }
                });
                add(new LinkedList<>(){
                    {
                        add(new CardContent("2", "Cars", R.drawable.ranking_category_cars));
                        add(new CardContent("Diesel", "Fuel Type", R.drawable.ranking_category_fuel_type));
                    }
                });
                add(new LinkedList<>(){
                    {
                        add(new CardContent("41 km/h", "Avg. Speed", R.drawable.ranking_category_avg_speed));
                        add(new CardContent("2000", "Avg. RPM", R.drawable.ranking_category_avg_rpm));
                    }
                });
            }
        };
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Double getEcoPoints() {
        return ecoPoints;
    }

    public void setEcoPoints(Double ecoPoints) {
        this.ecoPoints = ecoPoints;
    }

    public List<List<CardContent>> getDetailRankingCardContentList() {
        return detailRankingCardContentList;
    }
}
