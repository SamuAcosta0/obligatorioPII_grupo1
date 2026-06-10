package uy.edu.um.entities;

import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.list.Node;


public class Event {

    public enum EventType {
        CPU,
        RAM,
        DISK
    }
    private EventType type;
    private MyList<String> instructions;

    public Event(EventType type) {
        this.type         = type;
        this.instructions = new MyLinkedListImpl<>();
    }

    public EventType getType()              { return type; }
    public MyList<String> getInstructions() { return instructions; }

    public void addInstruction(String instruction) {
        instructions.add(instruction);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EVENT: ").append(type);
        sb.append(" | Instructions [");
        Node<String> current = instructions.getFirst();
        while (current != null) {
            sb.append(current.getValue());
            if (current.getNext() != null) {
                sb.append(", ");
            }
            current = current.getNext();
        }
        sb.append("]");
        return sb.toString();
    }
}