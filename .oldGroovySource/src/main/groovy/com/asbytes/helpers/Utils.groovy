package com.asbytes.helpers

import com.asbytes.Exchange
import com.asbytes.Main
import com.asbytes.Transaction
import org.apache.commons.io.filefilter.WildcardFileFilter
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import javax.xml.bind.annotation.adapters.XmlAdapter
import java.nio.file.Files
import java.nio.file.Path
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Stream

class Utils {
	static final String ANSI_DEFAULT  = '\u001B[0m'
	static final String ANSI_RED   = '\u001B[31m'
	static final String ANSI_GREEN = '\u001B[32m'
	static final String ANSI_WHITE = '\u001B[30m'
	static final String ANSI_GREY  = '\u001B[37m'

	static String find1stGroup(String src, String pattern, String[] group = null) {
		def groups = findGroups(src, pattern, group)
		if (groups.length > 0) {
			return groups.first()
		} else {
			return null
		}
	}

	static String[] findGroups(String src, String pattern, String[] group = null) {
		def matcher = (src =~ pattern)
		def results = []

		if (matcher.find()) {
			if (group) {
				group.each { results << matcher.group(it).trim() }
			} else {
				(1..matcher.groupCount()).each { results << matcher.group(it).trim() }
			}
		}

		return (results.any()) ? results : ""
	}

	static boolean isMatch(String src, String pattern) {
		def matcher = (src =~ pattern)
		return (matcher.find())
	}

	static Double toDouble(String val) {
		return Exchange.currencyToDouble(val)
	}

	static Double toDouble(val) {
		Double ret
		if (val instanceof Double) {
			ret = val
		} else if (val instanceof Float) {
			ret = (val as Float).toDouble()
		} else {
			ret = (val) ? toDouble("$val") : 0d
		}
		return ret
	}

	static Double roundUp(Object val, int precision = 2) {
		def first = new java.math.BigDecimal(val).setScale(3, java.math.RoundingMode.HALF_UP)
		def second = first.setScale(precision, java.math.RoundingMode.HALF_UP).doubleValue()
		return second
	}

	@SuppressWarnings(['GroovyOverlyComplexArithmeticExpression', 'GroovyUnusedIncOrDec', 'GroovyResultOfIncrementOrDecrementUsed'])
	public static int getRandomInt(int min, int max) {
		return (int)(Math.random() * (++max - min) + min);
	}


	public static File[] getFilesOfFolder(String folderPath, String fileFilter) {
		def folder = new File(folderPath)
		List<File> files = []
		files.addAll(folder.listFiles(new WildcardFileFilter(fileFilter) as FileFilter)  ?: [])
		files.addAll(folder.listFiles(new WildcardFileFilter(fileFilter.toUpperCase()) as FileFilter)  ?: [])
		files.addAll(folder.listFiles(new WildcardFileFilter(fileFilter.toLowerCase()) as FileFilter)  ?: [])

		Main.printout "checking ${folder.parentFile.name}/${folder.name}/$fileFilter... found ${files.size()} file(s)"
		return files.unique { left, right -> left.absolutePath.toLowerCase() <=> right.absolutePath.toLowerCase() }
	}

	static String toCurrency(Double value, boolean colored = true) {
		def eurSymbol = "EUR"

		if (value == null) {
			return "-,-- $eurSymbol"
		}
		def number = roundUp(value, 2)
		def pattern = new DecimalFormat("##,##0.00 $eurSymbol", new DecimalFormatSymbols(Locale.GERMAN))

		def col = (number < 0) ? ANSI_RED : ANSI_GREEN
		if (colored) {
			return col + pattern.format(number) + ANSI_DEFAULT
		} else {
			return pattern.format(number)
		}
	}

	static String clearColors(Object obj) {
		return obj.toString().replace([(Utils.ANSI_DEFAULT): '', (Utils.ANSI_GREEN): '', (Utils.ANSI_RED): ''])
	}

	static String fixIsinName(String name) {
		def map = getFixIsinName()
		return (name) ? name.replace(map).trim() : '<no name>'
	}

	static Map<String, String> getFixIsinName() {
		def cleanStr = [:]
		cleanStr.put('  ', ' ')
		cleanStr.put('iSharesV', 'iShares')
		cleanStr.put('iShares IV', 'iShares')
		cleanStr.put('iSharesIII', 'iShares')
		cleanStr.put('iSharesII', 'iShares')
		cleanStr.put('iSharesST.', 'iShares ')
		cleanStr.put('WisdomTree Comm. Securit. Ltd. DT.ZT08/Und.', '')
		cleanStr.put('WisdomTree Foreign Exchan. Ltd Dt.ZT14/Und.', '')
		cleanStr.put('WisdomTree Comm. Securit. Ltd. ZT15/Und.', '')
		cleanStr.put('ETFS Commodity Securities Ltd. ZT15/Und.', '')
		cleanStr.put('ETFS Metal Securities Ltd. DT.ZT07/Und.', '')
		cleanStr.put('HSBC Trinkaus & Burkhardt AG ', '')
		cleanStr.put('Vontobel Financial Products TurboC ', '')
		cleanStr.put('Vontobel Financial Products MiniL OpenEnd', 'Long')
		cleanStr.put('Société Générale Effekten GmbH ', '')
		cleanStr.put('O.End' , '')
		cleanStr.put('OpenEnd', '')
		cleanStr.put('ING Bank N.V. TurboS', 'Short')
		cleanStr.put('ING Bank N.V. TurboL', 'Long')
		cleanStr.put('TurboC', 'Long')
		cleanStr.put('TurboP', 'Put')
		cleanStr.put('ING Bank N.V. MiniL', 'Long')
		cleanStr.put('ING Bank N.V. MiniS', 'Short')
		cleanStr.put('UBS AG (London Branch) MiniL', 'Long')
		cleanStr.put('UBS AG (London Branch) MiniS', 'Short')
		cleanStr.put('BNP Paribas Em.-u.Handelsg.mbH TurboS', 'Short')
		cleanStr.put('BNP Paribas Em.-u.Handelsg.mbH TurboL', 'Long')
		cleanStr.put('MiniL OpenEnd ', 'Long')
		cleanStr.put('UBS AG (London Branch) TurboC ', '')
		cleanStr.put('Commerzbank AG TuBull ', '')
		cleanStr.put('Commerzbank AG QTuBe OpenEnd ', '')
		cleanStr.put('DZ BANK AG Deut.Zentral-Gen. ', '')
		cleanStr.put('iSh.', 'iShares')
		cleanStr.put('iShs', 'iShares')
		cleanStr.put('DT.ZT06/Und.', '')
		cleanStr.put(' Z 14(14/unl.)', ' ')
		cleanStr.put('Dt.Bank', 'Deutsche Bank')
		cleanStr.put('Coba', 'Commerzbank')
		// quirion
		cleanStr.put('Nam.-An.', '')
		cleanStr.put('Inhaber-Anteile', '')
		cleanStr.put('Acc o.N.', '')
		cleanStr.put('Nam.-Ant.', '')
		cleanStr.put('Registered Inc.Shs', '')
		cleanStr.put('Registered Shares', '')
		cleanStr.put('Registered Shs', '')
		cleanStr.put('Reg. Shares', '')
		cleanStr.put('Acc.hgd o.N', '')
		cleanStr.put('o.N.', '')
		cleanStr.put('  ', ' ')
		return cleanStr
	}

	static List<String> getIgnoreIsinName() {
		def ignoreStr = []
		ignoreStr << 'Registered '
		ignoreStr << 'Inhaber-'
		ignoreStr << 'Namens-Aktien'
		ignoreStr << 'Namens-Anteile'
		ignoreStr << 'Navne-'
		ignoreStr << 'Nam.Akt'
		ignoreStr << 'Azioni'
		ignoreStr << 'NK -'
		ignoreStr << 'o.N.'
		ignoreStr << 'Act. au Port'
		ignoreStr << 'Kündigungsrecht des'
		ignoreStr << '1 Nominale Stück'
		ignoreStr << 'Aandelen op'
		ignoreStr << 'Actions au'
		ignoreStr << 'DK -,'
		return ignoreStr
	}

	public static class XmlAdapters {
		public static class RateAdapter extends DoubleAdapter {
			@Override
			public String marshal(Double dbl) throws Exception {
				synchronized (this) {
					return (dbl ?: 0d).round(4)?.toString()
				}
			}

			@Override
			public Double unmarshal(String str) throws Exception {
				synchronized (this) {
					return (str.toDouble()).round(4)
				}
			}
		}

		public static class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {
			private DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

			@Override
			public String marshal(LocalDateTime dateTime) throws Exception {
				synchronized (this) {
					return dateTime.format(dateFormat);
				}
			}

			@Override
			public LocalDateTime unmarshal(String dateTime) throws Exception {
				synchronized (this) {
					return LocalDateTime.parse(dateTime, dateFormat);
				}
			}

		}

		public static class DoubleAdapter extends XmlAdapter<String, Double> {
			@Override
			public String marshal(Double dbl) throws Exception {
				synchronized (this) {
					// trying to detect bitpanda
					def num = (dbl ?: 0d)
					if (num.toString().startsWith('0.') && num != 0) {
						return num.round(8).toString()
					} else {
						return num.round(2).toString()
					}
				}
			}

			@Override
			public Double unmarshal(String str) throws Exception {
				synchronized (this) {
					return (str.toDouble())?.round(2)
				}
			}
		}
	}
}
