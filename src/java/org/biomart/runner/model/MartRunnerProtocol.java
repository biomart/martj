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

package org.biomart.runner.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.runner.controller.JobHandler;
import org.biomart.runner.exceptions.JobException;
import org.biomart.runner.exceptions.ProtocolException;

/**
 * Handles client communication and runs background jobs.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision$, $Date$, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public class MartRunnerProtocol {

	private static final String NEW_JOB = "NEW_JOB";

	private static final String BEGIN_JOB = "BEGIN_JOB";

	private static final String END_JOB = "END_JOB";

	private static final String LIST_JOBS = "LIST_JOBS";

	private static final String REMOVE_JOB = "REMOVE_JOB";

	// Short-cut for blank-line responses.
	private static final String NO_RESPONSE = "___NO_RESPONSE___";

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
		final PrintWriter bos;
		try {
			bos = new PrintWriter(clientSocket.getOutputStream(), true);
			bis = new BufferedReader(new InputStreamReader(clientSocket
					.getInputStream()));
			command = bis.readLine();
		} catch (final IOException e) {
			throw new ProtocolException(Resources.get("protocolIOProbs"), e);
		}
		Log.debug("Received command: " + command);
		try {
			// What do they want us to do?
			MartRunnerProtocol.class.getMethod("handle_" + command,
					new Class[] { BufferedReader.class, PrintWriter.class })
					.invoke(null, new Object[] { bis, bos });
		} catch (final InvocationTargetException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof ProtocolException)
				throw (ProtocolException) cause;
			else if (cause instanceof IOException)
				throw new ProtocolException(Resources.get("protocolIOProbs"),
						cause);
			else
				throw new ProtocolException(cause);
		} catch (final IllegalAccessException e) {
			Log.debug("Command recognised but unavailable, ignoring");
		} catch (final NoSuchMethodException e) {
			Log.debug("Command unrecognised, ignoring");
		}
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_NEW_JOB(final BufferedReader in,
			final PrintWriter out) throws ProtocolException, JobException,
			IOException {
		// Write out a new job ID.
		out.println(JobHandler.nextJobId());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_BEGIN_JOB(final BufferedReader in,
			final PrintWriter out) throws ProtocolException, JobException,
			IOException {
		JobHandler.beginJob(in.readLine());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_END_JOB(final BufferedReader in,
			final PrintWriter out) throws ProtocolException, JobException,
			IOException {
		JobHandler.endJob(in.readLine());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_REMOVE_JOB(final BufferedReader in,
			final PrintWriter out) throws ProtocolException, JobException,
			IOException {
		JobHandler.removeJob(in.readLine());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_LIST_JOBS(final BufferedReader in,
			final PrintWriter out) throws ProtocolException, JobException,
			IOException {
		final Collection jobIds = JobHandler.listJobs();
		final StringBuffer jobIdList = new StringBuffer();
		if (jobIds.isEmpty())
			jobIdList.append(MartRunnerProtocol.NO_RESPONSE);
		else
			for (final Iterator i = jobIds.iterator(); i.hasNext();) {
				jobIdList.append(i.next());
				if (i.hasNext())
					jobIdList.append(',');
			}
		out.println(jobIdList.toString());
	}

	/**
	 * Contains public methods for use by the client end of the protocol.
	 */
	public static class Client {

		private static Socket getClientSocket(final String host,
				final String port) throws IOException {
			return new Socket(host, Integer.parseInt(port));
		}

		/**
		 * Request a job ID that can be used for a new job. Does not actually
		 * begin the job. See {@link #beginJob(String, String, String)}.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @return the job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static String newJob(final String host, final String port)
				throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				bos.println(MartRunnerProtocol.NEW_JOB);
				return bis.readLine();
			} catch (final IOException e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			} finally {
				if (clientSocket != null)
					try {
						clientSocket.close();
					} catch (final IOException e) {
						// We don't care.
					}
			}
		}

		/**
		 * Flag that a job is beginning to be notified.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @param jobId
		 *            the job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void beginJob(final String host, final String port,
				final String jobId) throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				bos.println(MartRunnerProtocol.BEGIN_JOB);
				bos.println(jobId);
			} catch (final IOException e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			} finally {
				if (clientSocket != null)
					try {
						clientSocket.close();
					} catch (final IOException e) {
						// We don't care.
					}
			}
		}

		/**
		 * Flag that a job has ended being notified.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @param jobId
		 *            the job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void endJob(final String host, final String port,
				final String jobId) throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				bos.println(MartRunnerProtocol.END_JOB);
				bos.println(jobId);
			} catch (final IOException e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			} finally {
				if (clientSocket != null)
					try {
						clientSocket.close();
					} catch (final IOException e) {
						// We don't care.
					}
			}
		}

		/**
		 * Flag that a job can be removed and forgotten.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @param jobId
		 *            the job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void removeJob(final String host, final String port,
				final String jobId) throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				bos.println(MartRunnerProtocol.REMOVE_JOB);
				bos.println(jobId);
			} catch (final IOException e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			} finally {
				if (clientSocket != null)
					try {
						clientSocket.close();
					} catch (final IOException e) {
						// We don't care.
					}
			}
		}

		/**
		 * Request a list of current job IDs.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @return the new job ID.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static Collection listJobs(final String host, final String port)
				throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				bos.println(MartRunnerProtocol.LIST_JOBS);
				final String response = bis.readLine();
				if (response.equals(MartRunnerProtocol.NO_RESPONSE))
					return Collections.EMPTY_LIST;
				else
					return Arrays.asList(response.split(","));
			} catch (final IOException e) {
				throw new ProtocolException(Resources.get("protocolIOProbs"), e);
			} finally {
				if (clientSocket != null)
					try {
						clientSocket.close();
					} catch (final IOException e) {
						// We don't care.
					}
			}
		}
	}
}
