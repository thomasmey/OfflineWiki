package offlineWiki.utility;

import junit.framework.Assert;

import org.junit.Test;

public class HtmlUtilityTest {

	@Test
	public void decodeEntityTest() {
		String result = HtmlUtility.decodeEntities("&amp;");
		Assert.assertEquals("&",result);
	}
}
