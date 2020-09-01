package mt.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegexUtils {
	
	public static Matcher getMatcher(String str, String regex, Integer flags) {
		if (flags == null) {
			Pattern pattern = Pattern.compile(regex);
			return pattern.matcher(str);
		} else {
			Pattern pattern = Pattern.compile(regex, flags);
			return pattern.matcher(str);
		}
	}
	
	/**
	 * 找字符串第一个出现的位置
	 *
	 * @param str   字符串
	 * @param regex 正则表达式
	 * @param group 第几个括号里的值
	 * @return
	 */
	public static String findFirst(String str, String regex, Integer group) {
		return findFirst(str, regex, group, null);
	}
	
	public static String findFirst(String str, String regex, Integer group, Integer flags) {
		Matcher matcher = getMatcher(str, regex, flags);
		while (matcher.find()) {
			if (group != null) {
				return matcher.group(group);
			} else {
				return matcher.group();
			}
		}
		return null;
	}
	
	public static String findFirst(String str, String regex) {
		Integer group = null;
		return findFirst(str, regex, group);
	}
	
	/**
	 * 找字符串第一个出现的位置
	 *
	 * @param str    字符串
	 * @param regex  正则表达式
	 * @param groups 括号数组
	 * @return
	 */
	public static String[] findFirst(String str, String regex, Integer[] groups) {
		return findFirst(str, regex, groups, null);
	}
	
	public static String[] findFirst(String str, String regex, Integer[] groups, Integer flags) {
		Matcher matcher = getMatcher(str, regex, flags);
		while (matcher.find()) {
			if (groups != null) {
				String[] arr = new String[groups.length];
				for (int i = 0; i < groups.length; i++) {
					arr[i] = matcher.group(groups[i]);
				}
				return arr;
			} else {
				String[] arr = new String[1];
				arr[0] = matcher.group();
				return arr;
			}
		}
		return null;
	}
	
	/**
	 * 找所有符合正则条件的集合
	 *
	 * @param str
	 * @param regex
	 * @param group
	 * @return
	 */
	public static List<String> findList(String str, String regex, Integer group) {
		return findList(str, regex, group, null);
	}
	
	public static List<String> findList(String str, String regex, Integer group, Integer flags) {
		List<String> list = new ArrayList<String>();
		Matcher matcher = getMatcher(str, regex, flags);
		while (matcher.find()) {
			if (group != null) {
				list.add(matcher.group(group));
			} else {
				list.add(matcher.group());
			}
		}
		return list;
	}
	
	/**
	 * 找所有符合正则条件的集合
	 *
	 * @param str
	 * @param regex
	 * @param groups
	 * @return
	 */
	public static List<String[]> findList(String str, String regex, Integer[] groups) {
		return findList(str, regex, groups, null);
	}
	
	public static List<String[]> findList(String str, String regex, Integer[] groups, Integer flags) {
		List<String[]> list = new ArrayList<String[]>();
		Matcher matcher = getMatcher(str, regex, flags);
		while (matcher.find()) {
			if (groups != null) {
				String[] arr = new String[groups.length];
				for (int i = 0; i < groups.length; i++) {
					arr[i] = matcher.group(groups[i]);
				}
				list.add(arr);
			} else {
				String[] arr = new String[1];
				arr[0] = matcher.group();
				list.add(arr);
			}
		}
		return list;
	}
	
	public interface GroupDeal {
		String deal(String str, String[] group);
	}
	
	public static String replaceAll(String str, String regex, Integer[] group, GroupDeal groupDeal) {
		List<String[]> list = findList(str, regex, group);
		if (MyUtils.isEmpty(list)) {
			return str;
		}
		for (String[] groups : list) {
			str = groupDeal.deal(str, groups);
		}
		return str;
	}
}
