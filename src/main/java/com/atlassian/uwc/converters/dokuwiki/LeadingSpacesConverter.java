package com.atlassian.uwc.converters.dokuwiki;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.atlassian.uwc.converters.tikiwiki.RegexUtil;
import com.atlassian.uwc.converters.LeadingSpacesBaseConverter;
import com.atlassian.uwc.ui.Page;

/**
 * @author Laura Kolker
 * transforms sets of lines starting with two ws into code blocks
 */
public class LeadingSpacesConverter extends LeadingSpacesBaseConverter {

	Logger log = Logger.getLogger(this.getClass());

	protected String initialspacedelim = "  (?! *?[-*])"; //two spaces!

	@Override
	public void convert(Page page) {
		String input = page.getOriginalText();
		String converted = convertLeadingSpaces(input);
	}

	protected String convertLeadingSpaces(String input) {
		String replacement = getReplacementLoopUtil("<code>", "</code>");
		String regex = generateLeadingPattern(this.initialspacedelim);
		return convertLeadingSpacesLoop(input, regex, replacement);
	}

}
