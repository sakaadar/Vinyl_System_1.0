package via.vinylsystem.Util;

import via.vinylsystem.Model.RegistryEvent;

public interface AuditLog extends AutoCloseable
{
  void append(RegistryEvent e);
  @Override default void close() throws Exception{}
}
