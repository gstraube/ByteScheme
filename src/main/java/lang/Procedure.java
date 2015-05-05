package lang;

import java.util.List;

public interface Procedure {

    public abstract Datum apply(List<Datum> arguments);

}
