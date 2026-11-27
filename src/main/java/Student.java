/** A student member: up to 3 items, 14-day loans. */
public class Student extends Member {
    public Student(String id, String name) {
        super(id, name);
    }

    @Override
    public int getMaxItems() {
        return 3;
    }

    @Override
    public int getLoanPeriod() {
        return 14;
    }
}
