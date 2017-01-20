package org.ferrilidium.args;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ferrilidium.args.FeSimpleArgs.ParseState;
import org.ferrilidium.args.FeSimpleArgs.Result;
import org.junit.Test;

/*****************************************************************
 * Copyright [2017] [Cornelius Perkins]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributors:
 *    Cornelius Perkins - initial API and implementation and/or initial documentation
 *    
 * Author Cornelius Perkins (ccperkins at both github and bitbucket)
 ****************************************************************
 */

public class FeSimpleArgsTest {
	static boolean suppressStandardOutput=true;  // when true, results are only reported on failure via JUnit "fail".  When false, results are printed.
	void printResult(String str) {
		if (! suppressStandardOutput)
			System.out.println(str);
	}
	// 1) Takes an arbitrary String or String[] (i.e. doesn't use the arguments from the command line)
	@Test public void testParseAsString() {
		testParse ("-def --GHI -abc=fred", makeResult (makeMap (new String[] {"a", null, "b", null, "c", "fred", "d", null, "e", null, "f", null, "GHI", null}), null));
	}
	// 1) Takes an arbitrary String or String[] (i.e. doesn't use the arguments from the command line)
	@Test public void testParseFromTokens() {
		testParse (new String[] {"-def", "param1", "--GHI", "-a", "param2", "-b", "-c=fred", "param3"}, makeResult (makeMap (new String[] {"a", null, "b", null, "c", "fred", "d", null, "e", null, "f", null, "GHI", null}), makeList (new String[] {"param1", "param2", "param3"})));
	}
	@Test public void testParse() {
		testParse ("-def --GHI -a -b -c=fred", makeResult (makeMap (new String[] {"a", null, "b", null, "c", "fred", "d", null, "e", null, "f", null, "GHI", null}), null));
		testParse ("-def param1 --GHI -a param2 -b -c=fred -- param3", makeResult (makeMap (new String[] {"a", null, "b", null, "c", "fred", "d", null, "e", null, "f", null, "GHI", null}), makeList (new String[] {"param1", "param2", "param3"})));
		testParse ("-def param1 --GHI -a param2 -b -c=fred param3", makeResult (makeMap (new String[] {"a", null, "b", null, "c", "fred", "d", null, "e", null, "f", null, "GHI", null}), makeList (new String[] {"param1", "param2", "param3"})));
		testParse (new String[] {"-def", "param1", "--GHI", "-a", "param2", "-b", "-c=fred", "param3"}, makeResult (makeMap (new String[] {"a", null, "b", null, "c", "fred", "d", null, "e", null, "f", null, "GHI", null}), makeList (new String[] {"param1", "param2", "param3"})));
	}
	
		// suppressStandardOutput=false;
	@Test public void testParseValueWithQuotesFromArray() {
		String[] cliAsTokens = new String[] {"-n=\"foo bar baz\""};
		FeSimpleArgs.Result expected = makeResult(makeMap(new String[]{"n", "foo bar baz"}), null);
		testParse (cliAsTokens, expected);
	}
	@Test public void testParseValueWithQuotesFromString() {
		String cliAsString = "-n=\"foo bar baz\"";
		FeSimpleArgs.Result expected = makeResult(makeMap(new String[]{"n", "foo bar baz"}), null);
		testParse (cliAsString, expected);
	}
	
	void testParse(String args, FeSimpleArgs.Result expected) {
		FeSimpleArgs parser = new FeSimpleArgs();
		FeSimpleArgs.Result res = parser.parse(args);
		assertEqual ("testParseFromString: " + args, res, expected);
	}
	void testParse(String[] args, FeSimpleArgs.Result expected) {
		FeSimpleArgs parser = new FeSimpleArgs();
		FeSimpleArgs.Result res = parser.parse(args);
		assertEqual ("testParseFromTokens: " + dump(args), res, expected);
	}

	// 2A) Multiple single-letter options can be joined (e.g. -a -b -c to -abc)
	@Test public void test_A_B_C_Same_As_ABC() {
		FeSimpleArgs parser = new FeSimpleArgs();
		FeSimpleArgs.Result expected = makeResult(makeMap(new String[]{"a", null, "b", null, "c", null}), null);
		FeSimpleArgs.Result res1 = parser.parse("-abc");
		assertEqual ("testA_B_C_same_as_ABC", res1, expected);
		FeSimpleArgs.Result res2 = parser.parse("-a -b -c");
		assertEqual ("testA_B_C_same_as_ABC", res2, expected);
		assertEqual ("testA_B_C_same_as_ABC", res1, res2);
	}
	@Test public void test_A_B_CValue_Same_As_ABCValue() {
		FeSimpleArgs parser = new FeSimpleArgs();
		FeSimpleArgs.Result expected = makeResult(makeMap(new String[]{"a", null, "b", null, "c", "Value"}), null);
		FeSimpleArgs.Result res1 = parser.parse("-abc=Value");
		assertEqual ("testA_B_Cvalue_same_as_ABCvalue", res1, expected);
		FeSimpleArgs.Result res2 = parser.parse("-a -b -c=Value");
		assertEqual ("testA_B_Cvalue_same_as_ABCvalue", res2, expected);
	}

	// 2B) Supports long options (e.g. --message="Hello!")
	@Test public void testLongArgs() {
		FeSimpleArgs parser = new FeSimpleArgs();
		FeSimpleArgs.Result expected = makeResult(makeMap(new String[]{"message", "Hello!"}), null);
		FeSimpleArgs.Result res1 = parser.parse("--message=Hello!");
		assertEqual ("testLongArgs", res1, expected);
	}

	// 2C) Assumes all non-option-like bits at the end are parameters to be passed normally (e.g. -abc --long="Hello!" param1 param2 tells me that the parameters are param1 and param2)
	@Test public void testNonOptionLikeBitsAtEndAreParametersToBePassedNormally() {
		FeSimpleArgs parser = new FeSimpleArgs();
		FeSimpleArgs.Result expected = makeResult(makeMap(new String[]{"message", "Hello!", "a", null, "b", null, "c", null}), makeList(new String[] {"param1", "param2"}));
		FeSimpleArgs.Result res1 = parser.parse("-abc --message=\"Hello!\" param1 param2");
		assertEqual ("testNonOptionLikeParams", res1, expected);
	}

	// 2D) -- can be used to separate options from arguments (e.g. -ab --custom="hello" -- -file_starting_with_hyphen -another gives me the options/flags a, b, and custom with the appropriate values, and tells me that the arguments are -file_starting_with_hyphen and -another)
	@Test public void testDashDashSeparatesParameters() {
		FeSimpleArgs parser = new FeSimpleArgs();
		FeSimpleArgs.Result expected = makeResult(makeMap(new String[]{"message", "Hello!"}), makeList(new String[]{"-foo", "-bar"}));
		FeSimpleArgs.Result res1 = parser.parse("--message=\"Hello!\" -- -foo -bar");
		assertEqual ("testDashdashParams", res1, expected);
	}


	// 3) Whitespace is allowed in arguments
	@Test public void testWhitespace1() {   
		FeSimpleArgs parser = new FeSimpleArgs();
		FeSimpleArgs.Result expected = makeResult(makeMap(new String[]{"a", null, "b", null, "c", "Value"}), null);
		FeSimpleArgs.Result res = parser.parse("  -abc  =  Value  ");
		assertEqual ("testWhitespace1", res, expected);
	}
	@Test public void testWhitespace2() {   
		FeSimpleArgs parser = new FeSimpleArgs();
		FeSimpleArgs.Result expected = makeResult(makeMap(new String[]{"a", null, "b", null, "c", "Value"}), null);
		FeSimpleArgs.Result res = parser.parse(new String[] {"  -abc ", " =  ", " Value  "});
		assertEqual ("testWhitespace2", res, expected);
	}
	@Test public void testWhitespace3() {   
		FeSimpleArgs parser = new FeSimpleArgs();
		FeSimpleArgs.Result expected = makeResult(makeMap(new String[]{"n", "foo bar baz"}), null);
		//FeSimpleArgs.Result res = parser.parse(new String[] {"-n \"foo bar baz\""});
		FeSimpleArgs.Result res = parser.parse("-n=\"foo bar baz\"");
		assertEqual ("testWhitespace3", res, expected);
	}

	@Test public void testGetNextToken() {
		testGetNextToken(new String[]{"-name=value"}, new FeSimpleArgs.ParseState ("-name", "value", 1));
		testGetNextToken(new String[]{"-name=value", "another"}, new FeSimpleArgs.ParseState ("-name", "value", 1));
		testGetNextToken(new String[]{"-name=value", "another", "-aa"}, new FeSimpleArgs.ParseState ("-name", "value", 1));
		testGetNextToken(new String[]{"-name=value", "-aa"}, new FeSimpleArgs.ParseState ("-name", "value", 1));
		testGetNextToken(new String[]{"-name=\"multi part value\"", "-aa"}, new FeSimpleArgs.ParseState ("-name", "multi part value", 1));
		testGetNextToken(new String[]{"-name"}, new FeSimpleArgs.ParseState ("-name", null, 1));
		testGetNextToken(new String[]{"-name", "=value"}, new FeSimpleArgs.ParseState ("-name", "value", 2));
		testGetNextToken(new String[]{"-name", "=value", "-aa"}, new FeSimpleArgs.ParseState ("-name", "value", 2));
		testGetNextToken(new String[]{"-name", "=", "value"}, new FeSimpleArgs.ParseState ("-name", "value", 3));
		testGetNextToken(new String[]{"-name", "=", "value", "-aa"}, new FeSimpleArgs.ParseState ("-name", "value", 3));
		testGetNextToken(new String[]{"-name", "-aa"}, new FeSimpleArgs.ParseState ("-name", null, 1));
	}
	void testGetNextToken (String[] tokens, ParseState expected) {
		FeSimpleArgs parser = new FeSimpleArgs();
		ParseState ret = parser.getNextToken(tokens, 0);
		assertParseStateEquals ("testGetNextTokens@ " + dump(tokens), ret, expected);
	}
	void assertParseStateEquals (String testDesc, ParseState got, ParseState expected) {
		if (! parseStatesEqual(got, expected)) {
			String err = testDesc + ": Failure - got "+ got + ") - expected (" + expected + ")";
			printResult(err);
			fail(err);
		} else {
			printResult(testDesc + ": Success - got (" + got + ")" );
		}
	}


	String dump(String[] args) {
		StringBuilder sb = new StringBuilder();
		for (String arg: args) {
			if (sb.length() > 0) 
				sb.append ("], [");
			sb.append(arg);
		}
		return "[" + sb.toString() + "]";
	}



	@Test public void testTokenizeSimple3Tokens() {
		testTokenize("simple3tokens", "aaa    bbb  ccc", new String[] {"aaa", "bbb", "ccc"});
	// }
	// @Test public void testTokenizeQuoted2Tokens() {
		testTokenize("quoted2tokens", "aaa    \"bbb ccc\"", new String[] {"aaa", "bbb ccc"});
	// }
	// @Test public void testTokenizeQuotedNameEquals2Tokens() {
		testTokenize("quoted2tokens", "name=\"bbb ccc\"", new String[] {"name=bbb ccc"});
		testTokenize("quoted2tokens", "name= \"bbb ccc\"", new String[] {"name=bbb ccc"});
		testTokenize("quoted2tokens", "name = \"bbb ccc\"", new String[] {"name=bbb ccc"});
	// }
	
	// @Test public void testTokenize2TokensWithEscapedQuotes() {
		testTokenize("2tokenswithescapedquotes", "aaa    \\\"bbb ccc\\\"", new String[] {"aaa", "\\\"bbb", "ccc\\\""});
	// }
	// @Test public void testTokenizeWithValues() {
		testTokenize("withvalues", "-aaa=AAA    -bbb  = BBB -ccc= CCC -ddd =DDD", new String[] {"-aaa=AAA", "-bbb", "=", "BBB", "-ccc=", "CCC", "-ddd", "=DDD"});
	}

	public void testTokenize(String testDesc, String args, String[] expected) {
		FeSimpleArgs parser = new FeSimpleArgs();
		String[] tokens = parser.tokenize(args);
		boolean ret = stringArraysMatch(tokens, expected);
		if (ret)
			printResult("testTokenize@" + testDesc + ": Success - got (" + dump(tokens) + ")");
		else {
			String err = "testTokenize@" + testDesc + ": FAILURE - got (" + dump(tokens) + "), expected (" + dump(expected) + ")";
			printResult(err);
			// fail(err);
		}
	}


	boolean stringArraysMatch(String[] left, String[] right) {
		if (left.length != right.length) return false;
		Set<String> leftSet = new HashSet<>();
		for (String str: left)
			leftSet.add(str);
		for (String str: right)
			if (! leftSet.contains(str))
				return false;
		return true;
	}

	@Test public void testStringArraysMatch() {
		String[] l1 = new String[] {"aaa", "bbb", "ccc"};
		String[] l2 = new String[] {"aaa", "ccc", "bbb"};
		testStringArraysMatch("same-disordered", l1, l2, true);
		String[] l3 = new String[] {"aaa", "bbb", "ddd"};
		testStringArraysMatch("diff-samelength", l1, l3, false);
		String[] l4 = new String[] {"aaa", "ccc"};
		testStringArraysMatch("diff-difflength", l1, l4, false);

	}
	List<String> makeList(String[] strings) {
		List<String> ret = new ArrayList<>();
		for (String str: strings)
			ret.add(str);
		return ret;
	}
	public void testStringArraysMatch(String testDesc, String[] left, String[] right, boolean expected) {
		boolean ret = stringArraysMatch(left, right);
		if (ret == expected)
			printResult("testStringArraysMatch@ " + testDesc + ": Success ("+ ret + ")");
		else {
			String err = "testStringArraysMatch@ " + testDesc + ": Failure ("+ ret + ") - expected (" + expected + ")";
			printResult(err);
			fail(err);
		}
	}

	boolean listsMatch(List<String> left, List<String> right) {
		if (left.size() != right.size()) return false;
		Set<String> leftSet = new HashSet<>();
		leftSet.addAll(left);
		return (leftSet.containsAll(right));
	}
	@Test public void testListsMatch() {
		List<String> l1 = makeList( new String[] {"aaa", "bbb", "ccc"});
		List<String> l2 = makeList( new String[] {"aaa", "ccc", "bbb"});
		testListsMatch("same-disordered", l1, l2, true);
		List<String> l3 = makeList( new String[] {"aaa", "bbb", "ddd"});
		testListsMatch("diff-samelength", l1, l3, false);
		List<String> l4 = makeList( new String[] {"aaa", "ccc"});
		testListsMatch("diff-difflength", l1, l4, false);

	}

	public void testListsMatch(String testDesc, List<String> left, List<String> right, boolean expected) {
		boolean ret = listsMatch(left, right);
		if (ret == expected)
			printResult("testListsMatch@ " + testDesc + ": Success ("+ ret + ")");
		else {
			String err = "testListsMatch@ " + testDesc + ": Failure ("+ ret + ") - expected (" + expected + ")";
			printResult(err);
			fail(err);
		}
	}	

	boolean mapsMatch(Map<String,String> left, Map<String,String> right) {
		if (left.size() != right.size()) return false;
		if (! left.keySet().containsAll(right.keySet()))
			return false;
		for (String str: left.keySet()) 
			if (! stringsEqual(left.get(str), right.get(str)))
				return false;
		return true;
	}
	boolean stringsEqual(String left, String right) {
		if (left == null)
			return right == null;
		return (left.equals(right));
	}
	@Test public void testMapsMatch() {
		Map<String,String> m1 = makeMap( new String[] {"a", null, "b", null, "c", null}); 
		Map<String,String> m2 = makeMap( new String[] {"c", null, "a", null, "b", null});
		testMapsMatch("Map:same-disordered", m1, m2, true);

		Map<String,String> m3a = makeMap( new String[] {"c", "ccc", "a", "aaa", "b", "bbb"});
		Map<String,String> m3b = makeMap( new String[] {"a", "aaa", "b", "bbb", "c", "ccc"});
		testMapsMatch("Map:same-disordered-withvals", m3a, m3b, true);

		Map<String,String> m4 = makeMap( new String[] {"c", "ccc", "a", "aaa", "b", "BBB"}); 
		testMapsMatch("Map:diff-diffvals", m1, m4, false);

		Map<String,String> m5 = makeMap( new String[] {"c", null, "a", null, "d", null}); 
		testMapsMatch("Map:diff-samelength", m1, m5, false);

		Map<String,String> m6 = makeMap( new String[] {"c", null, "a", null, "c", null, "d", null}); 
		testMapsMatch("Map:diff-difflength", m1, m6, false);
	}
	/** NOTE: only works for even numbers of parts */
	Map<String,String> makeMap (String[] parts) { 
		if ((parts.length % 2) != 0)
			throw new IllegalArgumentException ("only works for even numbers of parts (key/value), and all keys must be non-null (" + parts.length + " supplied)");
		Map<String,String> ret = new HashMap<>();
		for (int ii=0; ii < parts.length; ii+= 2) {
			if (parts[ii] == null)
				throw new IllegalArgumentException ("all keys must be non-null");
			ret.put(parts[ii], parts[ii+1]);
		}
		return ret;
	}

	public void testMapsMatch(String testDesc, Map<String,String> left, Map<String,String> right, boolean expected) {
		boolean ret = mapsMatch(left, right);
		if (ret == expected)
			printResult("testMapsMatch@ " + testDesc + ": Success ("+ ret + ")");
		else {
			String err = "testMapsMatch@ " + testDesc + ": Failure ("+ ret + ") - expected (" + expected + ")";
			printResult(err);
			fail(err);
		}
	}	

	/** NOTE: under most conditions, the equals method is added to the class itself, but other than in testing, there is no need for comparison of results. */
	void assertEqual (String testDesc, FeSimpleArgs.Result got, FeSimpleArgs.Result expected) { 
		if (! resultsEqual(got, expected)) {
			String err = testDesc + "@ Failure - got "+ got + ") - expected (" + expected + ")";
			printResult(err);
			fail(err);
		} else {
			printResult(testDesc + "@ Success - got (" + got + ")" );
		}
	}

	/** NOTE: under most conditions, the equals method is added to the class itself, but other than in testing, there is no need for comparison of results. */
	boolean resultsEqual (FeSimpleArgs.Result left, FeSimpleArgs.Result right) { 
		return (listsMatch(left.params, right.params) && (mapsMatch(left.args, right.args)));
	}

	/** NOTE: under most conditions, the equals method is added to the class itself, but other than in testing, there is no need for comparison of parse state. */
	boolean parseStatesEqual (ParseState left, ParseState right) {
		return ( (left.name.equals(right.name))
				&& (bothAreNullOrEquals(left.value, right.value))
				&& (left.after == right.after)
				);
	}
	/** NOTE: under most conditions, the equals method is added to the class itself, but other than in testing, there is no need for comparison of parse state. */
	void assertEqual (String testDesc, ParseState got, ParseState expected) {
		if (! parseStatesEqual(got, expected)) {
			String err = testDesc + ": Failure - got "+ got + ") - expected (" + expected + ")";
			printResult(err);
			fail(err);
		} else {
			printResult(testDesc + ": Success - got (" + got + ")" );
		}

	}
	boolean bothAreNullOrEquals(String left, String right) {
		if (left == null) 
			return (right == null);
		else
			return left.equals(right);
	}

	@Test public void testTokenComplete() {
		testTokenComplete (new String[] {"-name=value"}, true);
		testTokenComplete (new String[] {"-name=value", "-other"}, true);
		testTokenComplete (new String[] {"-name=value", "foo"}, true);
		testTokenComplete (new String[] {"-name", "=wotsit"}, false);
		testTokenComplete (new String[] {"-name", "="}, false);
		testTokenComplete (new String[] {"-name="}, true); // complete but invalid
		testTokenComplete (new String[] {"-name", "-other"}, true);
		testTokenComplete (new String[] {"-name", "foo"}, true);
		testTokenComplete (new String[] {"-name"}, true);
	}
	void testTokenComplete(String[] tokens, boolean expected) {
		FeSimpleArgs parser = new FeSimpleArgs();
		StringBuilder sb = new StringBuilder();
		sb.append(tokens[0]);
		boolean ret = parser.tokenComplete(sb, tokens, 1);
		if (ret == expected)
			printResult("testTokenComplete@ " + dump(tokens) + ": Success ("+ ret + ")");
		else {
			String err = "testTokenComplete@ " + dump(tokens) + ": Failure ("+ ret + ") - expected (" + expected + ")";
			printResult(err);
			fail(err);
		}

	}

	public static Result makeResult(Map<String, String> args, List<String> params) {
		Result ret = new Result(args, params);
		return ret;
	}	
}

