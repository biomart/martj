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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;

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

	private static final String ADD_ACTION = "ADD_ACTION";

	private static final String GET_JOB_PLAN = "GET_JOB_PLAN";

	// Short-cut for ending messages and actions.
	private static final String END_MESSAGE = "___END_MESSAGE___";

	private static final String NEXT_ACTION = "___NEXT_ACTION___";

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
		final PrintWriter pwos;
		final InputStream is;
		final OutputStream os;
		try {
			is = clientSocket.getInputStream();
			os = clientSocket.getOutputStream();
			pwos = new PrintWriter(os, true);
			bis = new BufferedReader(new InputStreamReader(is));
			command = bis.readLine();
		} catch (final IOException e) {
			throw new ProtocolException(Resources.get("protocolIOProbs"), e);
		}
		Log.debug("Received command: " + command);
		try {
			// What do they want us to do?
			MartRunnerProtocol.class.getMethod(
					"handle_" + command,
					new Class[] { BufferedReader.class, InputStream.class,
							PrintWriter.class, OutputStream.class }).invoke(
					null, new Object[] { bis, is, pwos, os });
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
	 * @param inRaw
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @param outRaw
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_NEW_JOB(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		// Write out a new job ID.
		out.println(JobHandler.nextJobId());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param inRaw
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @param outRaw
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_BEGIN_JOB(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		JobHandler.beginJob(in.readLine());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param inRaw
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @param outRaw
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_END_JOB(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		JobHandler.endJob(in.readLine());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param inRaw
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @param outRaw
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_REMOVE_JOB(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		JobHandler.removeJob(in.readLine());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param inRaw
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @param outRaw
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_ADD_ACTION(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		final String jobId = in.readLine();
		final String[] sectionPath = in.readLine().split(",");
		final StringBuffer actions = new StringBuffer();
		final Collection finalActions = new ArrayList();
		String line;
		while (!((line = in.readLine()).equals(MartRunnerProtocol.END_MESSAGE))) {
			if (line.equals(MartRunnerProtocol.NEXT_ACTION)) {
				final String action = actions.toString();
				finalActions.add(action);
				Log.debug("Receiving action: " + action);
				actions.setLength(0);
			} else
				actions.append(line);
		}
		JobHandler.addActions(jobId, sectionPath, finalActions);
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param inRaw
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @param outRaw
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_LIST_JOBS(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		(new ObjectOutputStream(outRaw)).writeObject(JobHandler.listJobs());
	}

	/**
	 * Does something useful.
	 * 
	 * @param in
	 *            the input stream from the client.
	 * @param inRaw
	 *            the input stream from the client.
	 * @param out
	 *            the output stream back to the client.
	 * @param outRaw
	 *            the output stream back to the client.
	 * @throws ProtocolException
	 *             if the protocol fails.
	 * @throws JobException
	 *             if the job task requested fails.
	 * @throws IOException
	 *             if IO fails.
	 */
	public static void handle_GET_JOB_PLAN(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		(new ObjectOutputStream(outRaw)).writeObject(JobHandler.getJobPlan(in
				.readLine()));
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
				// TODO Write out JDBC connection details.
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
		 * Flag that a job is ending being notified.
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
		 * Request a list of current jobs as a {@link JobList} object.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @return the list of jobs.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static JobList listJobs(final String host, final String port)
				throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				bos.println(MartRunnerProtocol.LIST_JOBS);
				return (JobList) new ObjectInputStream(clientSocket
						.getInputStream()).readObject();
			} catch (final ClassNotFoundException e) {
				throw new ProtocolException(e);
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
		 * Add an action to a job.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @param jobId
		 *            the job ID.
		 * @param partition
		 *            the partition to add to.
		 * @param dataset
		 *            the dataset to add to.
		 * @param table
		 *            the table to add to.
		 * @param actions
		 *            the SQL statement(s) to add.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void addActions(final String host, final String port,
				final String jobId, final String partition,
				final String dataset, final String table, final String[] actions)
				throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				bos.println(MartRunnerProtocol.ADD_ACTION);
				bos.println(jobId);
				bos.println(partition + "," + dataset + "," + table);
				for (int i = 0; i < actions.length; i++) {
					bos.println(actions[i]);
					bos.println(MartRunnerProtocol.NEXT_ACTION);
				}
				bos.println(MartRunnerProtocol.END_MESSAGE);
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
		 * Retrieve a job plan.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @param jobId
		 *            the job ID.
		 * @return the job plan.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static JobPlan getJobPlan(final String host, final String port,
				final String jobId) throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				bos.println(MartRunnerProtocol.GET_JOB_PLAN);
				bos.println(jobId);
				return (JobPlan) new ObjectInputStream(clientSocket
						.getInputStream()).readObject();
			} catch (final ClassNotFoundException e) {
				throw new ProtocolException(e);
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
