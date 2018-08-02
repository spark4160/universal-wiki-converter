package com.atlassian.uwc.converters.dokuwiki;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;

import com.atlassian.uwc.converters.BaseConverter;
import com.atlassian.uwc.converters.tikiwiki.RegexUtil;
import com.atlassian.uwc.ui.Page;
import com.atlassian.uwc.ui.VersionPage;

public class TableRowColSpanConverter extends BaseConverter {

	Logger log = Logger.getLogger(this.getClass());
	@Override
	public void convert(Page page) {
		String input = page.getOriginalText();
		// if (input.contains("Force Indicator")) {
		// 	log.info("Force Indicator=" + input);
		// } else {
		// 	log.info("Force Dindicator");
		// }
		String tmpconverted = convertColspans(input);
		if (!(page instanceof VersionPage) && !input.equals(tmpconverted))
			log.debug("Colspans detected: '" + page.getName() + "' in space: " + page.getSpacekey());
		String converted = convertRowspans(tmpconverted);
		if (!(page instanceof VersionPage) && !tmpconverted.equals(converted))
			log.debug("Rowspans detected: '" + page.getName() + "' in space: " + page.getSpacekey());
		// Convert WRAPNEWLINEs back to newlines
		// converted = converted.replace("WRAPNEWLINE", "\n");
		converted = convertWraps(converted);
		page.setConvertedText(converted);
	}

	Pattern td = Pattern.compile("<t([dh])>(.*?)</t[dh]>", Pattern.DOTALL);
	Pattern uwctokencolspan = Pattern.compile("::" + PrepColSpansConverter.TOKENKEY + "(\\d+)::");
	protected String convertColspans(String input) {
		Matcher tdFinder = td.matcher(input);
		StringBuffer sb = new StringBuffer();
		boolean found = false;
		while (tdFinder.find()) {
			found = true;
			String type = tdFinder.group(1);
			String row = tdFinder.group(2);
			Matcher uwctokenFinder = uwctokencolspan.matcher(row);
			boolean found2 = false;
			String len = "";
			StringBuffer sb2 = new StringBuffer();
			while (uwctokenFinder.find()) {
				found2 = true;
				len = uwctokenFinder.group(1);
				String rep2= RegexUtil.handleEscapesInReplacement("");
				uwctokenFinder.appendReplacement(sb2, rep2);
			}
			if (found2) {
				uwctokenFinder.appendTail(sb2);
				row = sb2.toString();
			}
			else continue;
			String replacement = "<t"+type+" colspan='"+len+"'>"+row+"</t"+type+">";
			replacement = RegexUtil.handleEscapesInReplacement(replacement);
			tdFinder.appendReplacement(sb, replacement);
		}
		if (found) {
			tdFinder.appendTail(sb);
			return sb.toString();
		}
		return input;
	}

	Pattern uwctokenrowspan = Pattern.compile("::" + PrepRowSpansConverter.TOKENKEY + "(\\d+)::");
	Pattern rowspan = Pattern.compile(":::");
	protected String convertRowspans(String input) {
		Matcher tdFinder = td.matcher(input);
		StringBuffer sb = new StringBuffer();
		boolean found = false;
		while (tdFinder.find()) {
			found = true;
			String type = tdFinder.group(1);
			String row = tdFinder.group(2);
			Matcher rowspanFinder = rowspan.matcher(row);
			if (rowspanFinder.find()) {
				tdFinder.appendReplacement(sb, "");
				continue;
			}

			Matcher uwctokenFinder = uwctokenrowspan.matcher(row);
			boolean found2 = false;
			String len = "";
			StringBuffer sb2 = new StringBuffer();
			while (uwctokenFinder.find()) {
				found2 = true;
				len = uwctokenFinder.group(1);
				String rep2= RegexUtil.handleEscapesInReplacement("");
				uwctokenFinder.appendReplacement(sb2, rep2);
			}
			if (found2) {
				uwctokenFinder.appendTail(sb2);
				row = sb2.toString();
			}
			else continue;
			String replacement = "<t"+type+" rowspan='"+len+"'>"+row+"</t"+type+">";
			replacement = RegexUtil.handleEscapesInReplacement(replacement);
			tdFinder.appendReplacement(sb, replacement);
		}
		if (found) {
			tdFinder.appendTail(sb);
			return sb.toString();
		}
		return input;
	}

	Pattern wraps = Pattern.compile("(?s)WRAP64ENCODED ([^ ]*) WRAP64EXCODED");
	private String convertWraps(String input) {
		Matcher wrapsFinder = wraps.matcher(input);
		StringBuffer sb = new StringBuffer();
		boolean found = false;
		while (wrapsFinder.find()) {
			found = true;
			String all = wrapsFinder.group(1);
			log.info("wraps all=" + all);
			// String replacement = all.replace("\n", "WRAPNEWLINE");
			// String replacement = "NO BROKEN TABLES";
			// String replacement = "WRAP64ENCODED" + (new String(Encoder.encode(all.getBytes()), StandardCharsets.UTF_8)) + "WRAP64EXCODED";
			// Decoder decoder = new Decoder();
			String replacement = new String(Base64.getDecoder().decode(all), StandardCharsets.UTF_8);
			replacement = RegexUtil.handleEscapesInReplacement(replacement);
			log.info("wraps replacement=" + replacement);
			wrapsFinder.appendReplacement(sb, replacement);
		}
		// log.info("wraps found=" + found);
		if (found) {
			wrapsFinder.appendTail(sb);
			return sb.toString();
		}
		return input;
	}


}
