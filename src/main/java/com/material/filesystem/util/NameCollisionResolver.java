package com.material.filesystem.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class is used to resolve name collisions
 */
public class NameCollisionResolver {
  private static final String SUFFIX_SEPARATOR = "__";

  private final String _suffixSeparator;
  /**
   * This collision pattern handles files with or without extensions
   */
  private final Pattern _collisionPostfixPattern;

  public NameCollisionResolver() {
    this(SUFFIX_SEPARATOR);
  }

  public NameCollisionResolver(String suffixSeparator) {
    _suffixSeparator = suffixSeparator;
    _collisionPostfixPattern = Pattern.compile("^.+" + _suffixSeparator + "[0-9]+(\\.[a-z]+)?$");
  }

  /**
   * Resolve a name by creating a postfix on the name.
   *
   * @param name the collision name
   * @return a new name with a postfix of __1, or the next postfix in the sequence as detected by the current nme.
   * @throws NumberFormatException if there is an issue parsing the next sequence name.
   */
  public String resolve(String name) throws NumberFormatException {
    // matches a previously collided file name, so as to increment the sequence.
    Matcher matcher = _collisionPostfixPattern.matcher(name);
    if (matcher.matches()) {
      // get the next prefix in the sequence
      int indexOfUnderscores = name.lastIndexOf(_suffixSeparator);
      int extIdx = name.lastIndexOf('.');
      String extension = "";
      if (extIdx > -1) {
        extension = name.substring(extIdx);
      }
      int postfix = Integer.parseInt(
          name.substring(indexOfUnderscores + _suffixSeparator.length(), name.length() - extension.length()));
      return name.substring(0, indexOfUnderscores) + _suffixSeparator + (postfix + 1) + extension;
    } else {
      // find the extension
      int extIdx = name.lastIndexOf('.');
      if (extIdx > -1) {
        String extension = name.substring(extIdx);
        return name.substring(0, extIdx) + _suffixSeparator + "1" + extension;
      } else {
        return name + _suffixSeparator + "1";
      }
    }
  }
}
