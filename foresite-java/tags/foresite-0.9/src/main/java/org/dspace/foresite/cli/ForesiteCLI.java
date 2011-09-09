/*
 * ForesiteCLI.java
 *
 * Copyright (c) 2008, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.foresite.cli;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.dspace.foresite.OREParser;
import org.dspace.foresite.OREParserFactory;
import org.dspace.foresite.ORETransformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

/**
 * @Author Richard Jones
 */
public class ForesiteCLI
{
	public static void main(String[] args)
			throws Exception
	{
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("t", "transform", false, "Perform a format transformation.  Requires: -i, Optional: -o, -f");
		options.addOption("i", "in", true, "Input file to be parsed; required for -t");
		options.addOption("o", "out", true, "Output file to be written; if not specified output will be written to stdout");
		options.addOption("f", "format", true, "Format of incoming file to be parsed.  Defaults to RDF/XML");
		options.addOption("r", "result", true, "Format of output.  Defaults to RDF/XML");
		CommandLine line = parser.parse(options, args);

		ForesiteCLI cli = new ForesiteCLI();
		if (line.hasOption("t"))
		{
			if (line.hasOption("i"))
			{
				File input = new File(line.getOptionValue("i"));

				File output = null;
				if (line.hasOption("o"))
				{
					output = new File(line.getOptionValue("o"));
				}

				String inFormat = "RDF/XML";
				if (line.hasOption("f"))
				{
					inFormat = line.getOptionValue("f");
				}

				String outFormat = "RDF/XML";
				if (line.hasOption("r"))
				{
					outFormat = line.getOptionValue("r");
				}

				cli.transform(input, output, inFormat, outFormat);
			}

			System.exit(0);
		}

		ForesiteCLI.usage(options);
	}

	private void transform(File input, File output, String inFormat, String outFormat)
			throws Exception
	{
		FileInputStream fis = new FileInputStream(input);
		OutputStream os;
		if (output == null)
		{
			os = new ByteArrayOutputStream();
		}
		else
		{
			os = new FileOutputStream(output);
		}

		ORETransformer.transformToStream(inFormat, outFormat, fis, os);

		if (output == null)
		{
			System.out.println(os.toString());
		}

		os.flush();
		os.close();
	}

	private static void usage(Options options)
	{
		HelpFormatter help = new HelpFormatter();
		help.printHelp("ForesiteCLI -[t] -[iofr]", options);
	}
}
