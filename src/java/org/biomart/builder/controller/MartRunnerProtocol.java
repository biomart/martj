/*
 Copyright (C) 2006 EBI
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.biomart.builder.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.biomart.builder.exceptions.ProtocolException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * Handles client communication and runs background jobs.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by
 *          $Author$
 * @since 0.6
 */
public class MartRunnerProtocol {

	private static final String REQUEST_NEW_JOB = "REQUEST_NEW_JOB";

	private static final String BEGIN_JOB = "BEGIN_JOB";

	private static final String END_JOB = "END_JOB";

	/**
	 * Handles a client communication attempt. Receives an open socket and
	 * should return it still open.
	 * 
	 * @param clientSocket
	 *            the sccket to communicate over.
	 * @throws ProtocolException
	 *             if anything went wrong.
	 */
	public static void handleClient(final Socket clientSocket)
			throws ProtocolException {
		// Translates client requests into individual methods.
		final String command;
		final BufferedReader bis;
		try {
			bis = new BufferedReader(new InputStreamReader(clientSocket
					.getInputStream()));
			command = bis.readLine();
		} catch (final IOException e) {
			throw new ProtocolException(Resources.get("protocolIOProbs"), e);
		}
		Log.debug("Received command: "+command);
		// What do they want us to do?
		if (command.equals(MartRunnerProtocol.REQUEST_NEW_JOB))
			MartRunnerProtocol.handleRequestNewJob(clientSocket, bis);
		else if (command.equals(MartRunnerProtocol.BEGIN_JOB))
			MartRunnerProtocol.handleBeginJob(clientSocket, bis);
		else if (command.equals(MartRunnerProtocol.END_JOB))
			MartRunnerProtocol.handleEndJob(clientSocket, bis);
		// TODO The rest of the protocol!
		else
			Log.debug("Command unrecognised, ignoring");
	}

	private static void handleRequestNewJob(final Socket clientSocket,
			final BufferedReader bis) throws ProtocolException {
		// Write out a new job ID.
		try {
			final PrintWriter bos = new PrintWriter(clientSocket
					.getOutputStream());
			final int newJobID = MartRunner.requestNewJob();
			bos.write("" + newJobID);
			bos.flush();
		} catch (final IOException e) {
			throw new ProtocolException(Resources.get("protocolIOProbs"), e);
		}
	}

	private static void handleBeginJob(final Socket clientSocket,
			final BufferedReader bis) throws ProtocolException {
		String request = null;
		try {
			request = bis.readLine();
			MartRunner.beginJob(Integer.parseInt(request));
		} catch (final IOException e) {
			throw new ProtocolException(Resources.get("protocolIOProbs"), e);
		} catch (final NumberFormatException e) {
			throw new ProtocolException(Resources.get("jobIDInvalidNumber",
					request), e);
		}
	}

	private static void handleEndJob(final Socket clientSocket,
			final BufferedReader bis) throws ProtocolException {
		String request = null;
		try {
			request = bis.readLine();
			MartRunner.endJob(Integer.parseInt(request));
		} catch (final IOException e) {
			throw new ProtocolException(Resources.get("protocolIOProbs"), e);
		} catch (final NumberFormatException e) {
			throw new ProtocolException(Resources.get("jobIDInvalidNumber",
					request), e);
		}
	}

	/**
	 * Contains public methods for use by the client end of the protocol.
	 */
	public static class Client {
		/**
		 * Request a job ID that can be used for a new job. Does not actually
		 * begin the job. See {@link #beginJob(Socket, int)}.
		 * 
		 * @param serverSocket
		 *            the socket connected to the remote server.
		 * @return the new job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static int requestNewJob(final Socket serverSocket)
				throws ProtocolException {
			String response = null;
			try {
				final PrintWriter bos = new PrintWriter(serverSocket
						.getOutputStream());
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(serverSocket.getInputStream()));
				bos.write(MartRunnerProtocol.REQUEST_NEW_JOB);
				bos.flush();
				response = bis.readLine();
				return Integer.parseInt(response);
			} catch (final IOException e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			} catch (final NumberFormatException e) {
				throw new ProtocolException(Resources.get("jobIDInvalidNumber",
						response), e);
			}
		}

		/**
		 * Flag that a job is beginning to be notified.
		 * 
		 * @param serverSocket
		 *            the socket connected to the remote server.
		 * @param jobId
		 *            the job to flag.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void beginJob(final Socket serverSocket, final int jobId)
				throws ProtocolException {
			try {
				final PrintWriter bos = new PrintWriter(serverSocket
						.getOutputStream());
				bos.write(MartRunnerProtocol.BEGIN_JOB);
				bos.write("" + jobId);
				bos.flush();
			} catch (final IOException e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}

		/**
		 * Flag that a job has ended being notified.
		 * 
		 * @param serverSocket
		 *            the socket connected to the remote server.
		 * @param jobId
		 *            the job to flag.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void endJob(final Socket serverSocket, final int jobId)
				throws ProtocolException {
			try {
				final PrintWriter bos = new PrintWriter(serverSocket
						.getOutputStream());
				bos.write(MartRunnerProtocol.END_JOB);
				bos.write("" + jobId);
				bos.flush();
			} catch (final IOException e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			}
		}
	}
}
