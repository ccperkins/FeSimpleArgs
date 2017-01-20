package org.ferrilidium.args;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class FeSimpleArgs {
	/**
	 * Parses a stream of tokens into a Result containing arguments and parameters. Arguments can have a value (name=value) or flags (no value).
	 *     Argument names can be short or long (-abc is three short flags, while --abc is one long one)
	 *     Whitespace is permitted (around the equals sign or within a value)
	 *     Note: if the same argument name appears more than once, only the last will be kept.  That is, args [-a -a=foo -a=bar] is the same as just [-a=bar].
	 *     Order is not preserved among arguments (which are held in a Map), but are preserved in parameters (held in a List).  @see Result
	 * @param tokens
	 * @return
	 */
	public Result parse (String args) {
		String[] tokens = tokenize(args);
		return parse(tokens);
	}
	/**
	 * Parses a stream of tokens into a Result containing arguments and parameters. Arguments can have a value (name=value) or flags (no value).
	 *     Argument names can be short or long (-abc is three short flags, while --abc is one long one)
	 *     Whitespace is permitted (around the equals sign or within a value)
	 *     Note: if the same argument name appears more than once, only the last will be kept.  That is, args [-a -a=foo -a=bar] is the same as just [-a=bar].
	 *     Order is not preserved among arguments (which are held in a Map), but are preserved in parameters (held in a List).  @see Result
	 * @param tokens
	 * @return
	 */
	public Result parse (String[] tokens) {
		Result ret = new Result();
		for (int ii=0; ii < tokens.length; ii++) {
			String token = tokens[ii].trim();
			if ("--".equals(token)) { // separator.  Everything following this is a parameter
				for (int jj=ii+1; jj < tokens.length; jj++) {
					ret.params.add(tokens[jj]);
				}
				ii=tokens.length;
				break;

			} else if (token.startsWith("--")) { // long argument name.
				// strip the leading --, grab the argument name (watch for equals sign), and collect all the parts of the value
				ParseState nv = getNextToken (tokens, ii);
				ii = nv.after-1;
				String name = nv.name.substring(2);
				ret.args.put(name, nv.value);

			} else if (token.startsWith("-")) { // short argument name(or names).
				// strip the leading -, grab the argument name (watch for equals sign), break it into parts, and collect all the parts of the value
				ParseState nv = getNextToken (tokens, ii);
				ii = nv.after-1;
				String name = nv.name.substring(1);
				for (int ff=0; ff < name.length()-1; ff++) {
					ret.args.put("" + name.charAt(ff), null);
				}
				ret.args.put("" + name.charAt(name.length()-1), nv.value);

			} else { // this is probably an error: from the requirements, we shouldn't be in this position if the arguments are valid.  Treating as parameter for now
				ret.params.add(token);
			}
		}
		return ret;

	}
	
	/**
	 * Holds the results of parsing.  Arguments (which may take an optional value) are held in the map, and parameters are kept in the list.
	 * @author Cornelius Perkins (ccperkins at bitbucket and github)
	 */
	public static class Result {
		public final Map<String, String> args;
		public final List<String> params;
		public Result(Map<String, String> args, List<String> params) {
			super();
			if (args == null)
				this.args = new HashMap<String,String>();
			else
				this.args = args;
			if (params == null)
				this.params = new ArrayList<String>();
			else
				this.params = params;
		}
		public Result() {
			this (null, null);
		}
		public String toString() {
			StringBuilder sbParams = new StringBuilder();
			for (String param: params) {
				if (sbParams.length() > 0)
					sbParams.append(", ");
				sbParams.append(param);
			}
			StringBuilder sbArgs = new StringBuilder();
			for (String arg: args.keySet()) {
				if (sbArgs.length() > 0)
					sbArgs.append(", ");
				if (args.get(arg) == null)
					sbArgs.append(arg);
				else
					sbArgs.append(arg).append("=").append(args.get(arg));
			}
			return "Args=[" + sbArgs.toString() + "]; Params=[" + sbParams.toString() + "]";
		}
	}


	private final String regex = "\"([^\"]*)\"|(\\S+)";
	private final Pattern pattern = Pattern.compile(regex);

	/*
	 * Separates a string into tokens. 
	 * NOTE: would be private except for the needs of unit testing.
	 * @param args
	 * @return
	 */
	String[] tokenize (String args) {
		List<String> accum = new ArrayList<>();
		Matcher m = pattern.matcher(args);
		while (m.find()) {
			if (m.group(1) != null) {
				accum.add(m.group(1));
			} else {
				accum.add(m.group(2));
			}
		}
		String[] ret= new String[accum.size()];
		for (int ii=0; ii < accum.size(); ii++)
			ret[ii] = accum.get(ii);
		return ret;
	}


	/* Creates a "name-value" pair from the given tokens starting with the given index. 
	 * Note that if you don't make sure the first token includes a name (is not just
	 * a solitary dash or double-dash), you're gonna have a bad time.
	 * NOTE: would be private except for the needs of unit testing.
	 */
	ParseState getNextToken(String[] tokens, int idxFirstValue) {
		// Look ahead for next arg or end of tokens
		StringBuilder sb = new StringBuilder();
		int idxLastValue = idxFirstValue;
		sb.append(tokens[idxLastValue++].trim());
		/*
		 * NOTES: after a refactor we're failing.   This loop is finding {-name} but breaking on {-name -aa}.
		 */
		while (! tokenComplete (sb, tokens, idxLastValue)) {
			sb.append(" ");
			sb.append(tokens[idxLastValue++]);
		}

		// our name or name=value pair is now in sb.  
		String[] parts = sb.toString().split("[= ]");
		// the first part is the name, and any non-empty parts after make up the value.
		String name = parts[0].trim();
		StringBuilder value = new StringBuilder();
		for (int ii=1; ii < parts.length; ii++) {
			if (parts[ii].trim().length() > 0) {
				if (value.length() > 0)
					value.append(" ");
				value.append(parts[ii].trim());
			}
		}
		ParseState nv;
		if (value.length() > 0) {
			String val = value.toString();
			// if there are quotes, strip them
			if (val.indexOf('"') >= 0) {
				val = val.substring(val.indexOf('"')+1, val.lastIndexOf('"'));
			}
			nv = new ParseState (name, val, idxLastValue);
		}
		else
			nv = new ParseState (name, null, idxLastValue);
		return nv;
	}

	/* Returns true if the given token is "complete", which either means we have name=value or just name and there's no =value.
	 * Note that value can be a quoted string, and that it's possible for the start and ending quote to be in different tokens.
	 * NOTE: would be private except for the needs of unit testing.
	 */
	boolean tokenComplete (StringBuilder token, String[] tokens, int idx) {
		// If there are no more, we're done
		if (idx >= tokens.length)
			return true;
		// If we have name=value, we're done (but remember that value may have more than one part)
		if (token.indexOf("=") > 0) {
			String[] parts = token.toString().split("=");
			if (parts.length == 2) {
				if (parts[1].indexOf('"') >= 0) { // there's a quote.  Are there two?
					return (parts[1].lastIndexOf('"') > parts[1].indexOf('"'));
				} else { 
					return (parts[1].trim().length() > 0); // we either have name=value or name=
				}
			} else // name= but no value
				return false;
		}
		// So now we know we just have name.  if next begins with = we continue (not complete), otherwise we are.
		return (! tokens[idx].trim().startsWith("="));
	}

	/*
	 * Internal class, used to hold parsing state.
	 * NOTE: would be private except for the needs of unit testing.
	 */
	static class ParseState {
		public final String name;
		public final String value;
		public final int after;
		public ParseState(String name, String value, int after) {
			super();
			this.name = name;
			this.value = value;
			this.after = after;
		}
		@Override
		public String toString() {
			if (value == null)
				return name + "(" + after + ")";
			else
				return name + "={" + value + "}(" + after + ")";
		}
	}
}

