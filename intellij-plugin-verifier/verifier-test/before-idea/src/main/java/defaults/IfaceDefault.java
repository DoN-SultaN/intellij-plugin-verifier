package defaults;

public interface IfaceDefault extends Iface {
  @Override
  default void defaultMethod() {
    //implementation
  }
}
