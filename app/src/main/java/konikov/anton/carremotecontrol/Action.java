package konikov.anton.carremotecontrol;

public enum Action {
    MOVE_UP(100), MOVE_DOWN(200), MOVE_LEFT(300), MOVE_RIGHT(400), MOVE_STOP(500), RESET_RUDDER(600), RESET_MOTION(700);

    private final int id;

    Action(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
