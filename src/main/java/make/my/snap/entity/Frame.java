package make.my.snap.entity;


import com.squareup.moshi.Json;

public class Frame {
    @Json(name = "x") public float x;
    @Json(name = "y") public float y;
    @Json(name = "deltaX") public float deltaX;
    @Json(name = "deltaY") public float deltaY;
    @Json(name = "jerkX") public float jerkX;
    @Json(name = "jerkY") public float jerkY;
    @Json(name = "gcdErrorX") public float gcdErrorX;
    @Json(name = "gcdErrorY") public float gcdErrorY;

    public Frame(float x, float y, float deltaX, float deltaY, float jerkX, float jerkY, float gcdErrorX, float gcdErrorY) {
        this.x = x;
        this.y = y;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.jerkX = jerkX;
        this.jerkY = jerkY;
        this.gcdErrorX = gcdErrorX;
        this.gcdErrorY = gcdErrorY;
    }
}