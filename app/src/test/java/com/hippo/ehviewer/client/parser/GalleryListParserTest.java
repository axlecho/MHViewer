/*
 * Copyright 2019 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.client.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.hippo.ehviewer.client.EhUtils;
import edu.emory.mathcs.backport.java.util.Arrays;
import java.io.InputStream;
import java.util.List;
import okio.BufferedSource;
import okio.Okio;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class GalleryListParserTest {

  private static final String E_MINIMAL = "GalleryListParserTestEMinimal.html";
  private static final String E_MINIMAL_PLUS = "GalleryListParserTestEMinimalPlus.html";
  private static final String E_COMPAT = "GalleryListParserTestECompat.html";
  private static final String E_EXTENDED = "GalleryListParserTestEExtended.html";
  private static final String E_THUMBNAIL = "GalleryListParserTestEThumbnail.html";

  private static final String EX_MINIMAL = "GalleryListParserTestExMinimal.html";
  private static final String EX_MINIMAL_PLUS = "GalleryListParserTestExMinimalPlus.html";
  private static final String EX_COMPAT = "GalleryListParserTestExCompat.html";
  private static final String EX_EXTENDED = "GalleryListParserTestExExtended.html";
  private static final String EX_THUMBNAIL = "GalleryListParserTestExThumbnail.html";

  @ParameterizedRobolectricTestRunner.Parameters(name = "{index}-{0}")
  public static List data() {
    return Arrays.asList(new Object[][] {
        { E_MINIMAL },
        { E_MINIMAL_PLUS },
        { E_COMPAT },
        { E_EXTENDED },
        { E_THUMBNAIL },
        { EX_MINIMAL },
        { EX_MINIMAL_PLUS },
        { EX_COMPAT },
        { EX_EXTENDED },
        { EX_THUMBNAIL },
    });
  }

  private String file;

  public GalleryListParserTest(String file) {
    this.file = file;
  }
}
