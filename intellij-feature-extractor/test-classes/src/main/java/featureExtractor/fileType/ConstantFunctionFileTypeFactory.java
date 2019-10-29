package featureExtractor.fileType;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;

public class ConstantFunctionFileTypeFactory extends FileTypeFactory {
  @Override
  public void createFileTypes(FileTypeConsumer consumer) {
    consumer.consume(null, constantFunction());
  }

  final public String constantFunction() {
    return ".constantValue";
  }

}


