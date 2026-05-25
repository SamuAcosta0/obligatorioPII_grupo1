package uy.edu.um.entities;

import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.list.MyLinkedListImpl;

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
        for (int i = 0; i < instructions.size(); i++) {
            sb.append(instructions.get(i));
            if (i < instructions.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}