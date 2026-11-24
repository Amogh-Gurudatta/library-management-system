/** Base class for library members. Each subclass decides its own limit and loan period. */
public abstract class Member {
    private final String id;
    private final String name;

    protected Member(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** Maximum number of items this member may hold at once. */
    public abstract int getMaxItems();

    /** Number of days a loan lasts before it is due. */
    public abstract int getLoanPeriod();
}
