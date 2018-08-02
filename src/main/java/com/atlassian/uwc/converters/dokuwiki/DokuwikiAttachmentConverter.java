package com.atlassian.uwc.converters.dokuwiki;

import java.io.File;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

import com.atlassian.uwc.converters.BaseConverter;
import com.atlassian.uwc.converters.tikiwiki.RegexUtil;
import com.atlassian.uwc.ui.Page;

public class DokuwikiAttachmentConverter extends HierarchyImageConverter {

	@Override
	public void convert(Page page) {
		String converted = convertAttachments(page);
		// TODO: convert nspages to child macro (do this manually)
		converted = convertNSPages(converted);
		// TODO: fix <nowiki></nowiki> to be plain text unescaped (do this manually)
		page.setConvertedText(converted);
	}

	Pattern attachment = Pattern.compile("(?s)\\{\\{(.*?)\\}\\}");
	Pattern parts = Pattern.compile("([^|?]+)(?:([?|])(.*))?");
	Pattern type = Pattern.compile(".*?[.]([^.]+)$");
	private String convertAttachments(Page page) {
		String attdir = getAttachmentDirectory();
		log.info("attdir=" + attdir);
		String relpath = page.getPath().replaceFirst("[^\\"+File.separator+"]*$", "");
		String input = page.getOriginalText();
		Matcher attachmentFinder = attachment.matcher(input);
		StringBuffer sb = new StringBuffer();
		boolean found = false;
		HashSet<File> candidates = new HashSet<File>();
		while (attachmentFinder.find()) {
			found = true;
			String all = attachmentFinder.group(1);
			log.info("all=" + all);
			String replacement = "";
			Matcher partsFinder = parts.matcher(all);
			if (partsFinder.find()) {
				String dokupath = partsFinder.group(1);
				String fileRelPath = createRelativePath(dokupath, relpath);
				File file = new File(attdir + File.separator + fileRelPath);
				if (!file.exists()) {
					file = altCase(file, attdir);
				}
				candidates.add(file);
				replacement = file.getName();
				Matcher typeFinder = type.matcher(file.getName());
				if (typeFinder.find()) {
					String filetype = typeFinder.group(1);
					if (isImage(file.getName())) {
						String size = getSizeData(partsFinder);
						replacement = "!" + replacement + size + "!";
					}
					else {
						replacement = "^" + replacement;
						String alias = getAlias(partsFinder);
						replacement = "[" + alias + replacement + "]";
					}
				}
			}
			replacement = RegexUtil.handleEscapesInReplacement(replacement);
			log.info("replacement=" + replacement);
			attachmentFinder.appendReplacement(sb, replacement);
		}
		log.info("found=" + found);
		if (found) {
			attachmentFinder.appendTail(sb);
			for (File file : candidates) {
				log.debug("Found attachment: " + file.getName());
				page.addAttachment(file);
			}
			// log.info("sb.toString()=" + sb.toString());
			return sb.toString();
		}
		return input;
	}

	protected File altCase(File file, String attdir) {
		String name = file.getName();

		// Check in root of attachments directory
		File rootFile = new File(attdir + File.separator + name);
		if (rootFile.exists()) {
			return rootFile;
		}

		// Check in parent
		File parent = file.getParentFile();
		if (parent == null || !parent.exists()) {
			log.warn("File probably does not exist: " + file.getAbsolutePath());
			return file;
		}
		File[] files = parent.listFiles();
		if (files == null) {
			log.warn("File probably does not exist: " + file.getAbsolutePath());
			return file;
		}
		for (File realFile : files) {
			if (name.equalsIgnoreCase(realFile.getName())) return realFile;
		}
		log.warn("File probably does not exist: " + file.getAbsolutePath());
		return file;
	}

	private String getSizeData(Matcher partsFinder) {
		String hasOtherData = partsFinder.group(2);
		String otherData = partsFinder.group(3);
		String sizedata = "";
		if (hasOtherData != null && otherData != null && !"".equals(otherData)) {
			if (hasOtherData.equals("?")) {
				Matcher paramFinder = params.matcher(otherData);
				if (paramFinder.find()) {
					String width = paramFinder.group(1);
					String height = paramFinder.group(3);
					sizedata = "|width=" + width + "px" + (height != null?",height="+height + "px":"");
				}
			}
		}
		return sizedata;
	}

	public String getAlias(Matcher partsFinder) {
		String hasOtherData = partsFinder.group(2);
		String otherData = partsFinder.group(3);
		String alias = "";
		if (hasOtherData != null && otherData != null && !"".equals(otherData)) {
			if (hasOtherData.equals("|")) {
				alias = otherData + "|";
			}
		}
		return alias;
	}

	Pattern isRelative = Pattern.compile("^:[^:]+$");
	Pattern notfilename = Pattern.compile("^(.*?)(:[^:]+)$");
	protected String createRelativePath(String input, String relativePath) {
		input = input.trim();
		log.info("input=" + input);
		if (isRelative.matcher(input).find()) {
			String ignorable = getProperties().getProperty("filepath-hierarchy-ignorable-ancestors", "");
			log.info("ignorable=" + ignorable);
			relativePath = relativePath.replaceFirst("\\Q"+ignorable+"\\E", "");
			if (!relativePath.endsWith(File.separator)) relativePath += File.separator;
			log.info("relativePath=" + relativePath);
			relativePath = relativePath.replace("\\", "\\\\");
			log.info("relativePath=" + relativePath);
			input = input.replaceFirst(":", relativePath);
		}
		else { //deal with case sensitivity in parent path
			Matcher pathFinder = notfilename.matcher(input);
			if (pathFinder.find()) {
				String parentpath = pathFinder.group(1);
				String replacement = parentpath.toLowerCase();
				if (!replacement.equals(parentpath))
					log.debug("Attachments: transformed to lower case parent relative path: " + replacement);
				input = replacement + pathFinder.group(2);
			}
		}
		log.info("File.separator=" + File.separator);
		// input = input.replaceAll(":", File.separator);
		input = input.replaceAll("\\:", "\\\\");
		if (!input.startsWith(File.separator)) input = File.separator + input;
		return input;
	}

	Pattern nspages = Pattern.compile("(?s)(\\<nspages[^\\>]+\\>)");
	private String convertNSPages(String input) {
		Matcher nspagesFinder = nspages.matcher(input);
		StringBuffer sb = new StringBuffer();
		boolean found = false;
		while (nspagesFinder.find()) {
			found = true;
			String all = nspagesFinder.group(1);
			log.info("nspages all=" + all);
			String replacement = "<p><ac:structured-macro " +
				"ac:name=\"children\" " +
				"ac:schema-version=\"2\" " +
				"ac:macro-id=\"" + UUID.randomUUID().toString() + "\" /></p>";
			replacement = RegexUtil.handleEscapesInReplacement(replacement);
			log.info("nspages replacement=" + replacement);
			nspagesFinder.appendReplacement(sb, replacement);
		}
		log.info("nspages found=" + found);
		if (found) {
			nspagesFinder.appendTail(sb);
			return sb.toString();
		}
		return input;
	}

}
