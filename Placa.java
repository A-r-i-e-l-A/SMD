public class Placa {
    private String ID;
    private String type;
    private String outline;
    private double posX;
    private double posY;
    private double rotation;
    private boolean flip;
    private int files;

    public Placa(String ID, String type, String outline, double posX, double posY, double rotation, boolean flip,
            int files) {
        this.ID = ID;
        this.type = type;
        this.outline = outline;
        this.posX = posX;
        this.posY = posY;
        this.rotation = rotation;
        this.flip = flip;
        this.files = files;
    }

    // Getters and setters
    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public boolean isFlip() {
        return flip;
    }

    public void setFlip(boolean flip) {
        this.flip = flip;
    }

    public int getFiles() {
        return files;
    }

    public void setFiles(int files) {
        this.files = files;
    }

    // toString method to print Placa details
    @Override
    public String toString() {
        return "Placa{" +
                "ID=" + ID +
                ", type='" + type + '\'' +
                ", outline='" + outline + '\'' +
                ", posX=" + posX +
                ", posY=" + posY +
                ", rotation=" + rotation +
                ", flip=" + flip +
                ", files=" + files +
                '}';
    }
}
