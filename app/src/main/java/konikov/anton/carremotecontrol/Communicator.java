package konikov.anton.carremotecontrol;

public interface Communicator {
    void startCommunication();
    void write(String message);
    void stopCommunication();
}
