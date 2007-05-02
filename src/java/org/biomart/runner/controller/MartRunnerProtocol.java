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

package org.biomart.runner.controller;

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
import java.util.Iterator;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.runner.exceptions.JobException;
import org.biomart.runner.exceptions.ProtocolException;
import org.biomart.runner.model.JobList;
import org.biomart.runner.model.JobPlan;
import org.biomart.runner.model.JobPlan.JobPlanSection;

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

	private static final String SET_ACTIONS = "SET_ACTIONS";

	private static final String GET_ACTIONS = "GET_ACTIONS";

	private static final String EMAIL_ADDRESS = "EMAIL_ADDRESS";

	private static final String THREAD_COUNT = "THREAD_COUNT";

	private static final String START_JOB = "START_JOB";

	private static final String STOP_JOB = "STOP_JOB";

	private static final String QUEUE = "QUEUE";

	private static final String UNQUEUE = "UNQUEUE";

	// Short-cut for ending messages and actions.
	private static final String END_MESSAGE = "___END_MESSAGE___";

	private static final String NEXT = "___NEXT___";

	private static final String OK = "___OK___";

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
		final String jobId = in.readLine();
		final String jdbcDriverClassName = in.readLine();
		final String jdbcURL = in.readLine();
		final String jdbcUsername = in.readLine();
		final String jdbcPassword = in.readLine();
		JobHandler.beginJob(jobId, jdbcDriverClassName, jdbcURL, jdbcUsername,
				jdbcPassword.equals(null) ? null : jdbcPassword);
		out.println(MartRunnerProtocol.OK);
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
		out.println(MartRunnerProtocol.OK);
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
		out.println(MartRunnerProtocol.OK);
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
	public static void handle_SET_ACTIONS(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		final String jobId = in.readLine();
		final String[] sectionPath = in.readLine().split(",");
		final StringBuffer actions = new StringBuffer();
		final Collection finalActions = new ArrayList();
		String line;
		while (!(line = in.readLine()).equals(MartRunnerProtocol.END_MESSAGE))
			if (line.equals(MartRunnerProtocol.NEXT)) {
				final String action = actions.toString();
				finalActions.add(action);
				Log.debug("Receiving action: " + action);
				actions.setLength(0);
			} else
				actions.append(line);
		JobHandler.setActions(jobId, sectionPath, finalActions, false);
		out.println(MartRunnerProtocol.OK);
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
	public static void handle_QUEUE(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		final String jobId = in.readLine();
		final Collection identifiers = new ArrayList();
		String line;
		while (!(line = in.readLine()).equals(MartRunnerProtocol.END_MESSAGE))
			identifiers.add(line);
		JobHandler.queue(jobId, identifiers);
		out.println(MartRunnerProtocol.OK);
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
	public static void handle_UNQUEUE(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		final String jobId = in.readLine();
		final Collection identifiers = new ArrayList();
		String line;
		while (!(line = in.readLine()).equals(MartRunnerProtocol.END_MESSAGE))
			identifiers.add(line);
		JobHandler.unqueue(jobId, identifiers);
		out.println(MartRunnerProtocol.OK);
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
		(new ObjectOutputStream(outRaw)).writeObject(JobHandler.getJobList());
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
	public static void handle_GET_ACTIONS(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		final String jobId = in.readLine();
		final String sectionId = in.readLine();
		(new ObjectOutputStream(outRaw)).writeObject(new ArrayList(JobHandler
				.getActions(jobId, sectionId).values()));
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
	public static void handle_EMAIL_ADDRESS(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		final String jobId = in.readLine();
		final String email = in.readLine();
		JobHandler.setEmailAddress(jobId, email);
		out.println(MartRunnerProtocol.OK);
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
	public static void handle_THREAD_COUNT(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		final String jobId = in.readLine();
		final int threadCount = Integer.parseInt(in.readLine());
		JobHandler.setThreadCount(jobId, threadCount);
		out.println(MartRunnerProtocol.OK);
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
	public static void handle_START_JOB(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		JobHandler.startJob(in.readLine());
		out.println(MartRunnerProtocol.OK);
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
	public static void handle_STOP_JOB(final BufferedReader in,
			final InputStream inRaw, final PrintWriter out,
			final OutputStream outRaw) throws ProtocolException, JobException,
			IOException {
		JobHandler.stopJob(in.readLine());
		out.println(MartRunnerProtocol.OK);
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
		 * begin the job.
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
		 * @param jdbcDriverClassName
		 *            the JDBC driver classname for the server the job will run
		 *            against.
		 * @param jdbcURL
		 *            the JDBC URL of the server the job will run against.
		 * @param jdbcUsername
		 *            the JDBC username for the server the job will run against.
		 * @param jdbcPassword
		 *            the JDBC password for the server the job will run against.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void beginJob(final String host, final String port,
				final String jobId, final String jdbcDriverClassName,
				final String jdbcURL, final String jdbcUsername,
				final String jdbcPassword) throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				bos.println(MartRunnerProtocol.BEGIN_JOB);
				bos.println(jobId);
				bos.println(jdbcDriverClassName);
				bos.println(jdbcURL);
				bos.println(jdbcUsername);
				bos.println(jdbcPassword);
				bis.readLine(); // OK
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
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				bos.println(MartRunnerProtocol.END_JOB);
				bos.println(jobId);
				bis.readLine(); // OK
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
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				bos.println(MartRunnerProtocol.REMOVE_JOB);
				bos.println(jobId);
				bis.readLine(); // OK
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
		 * Request a list of current jobs as {@link JobPlan} objects.
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
		public static void setActions(final String host, final String port,
				final String jobId, final String partition,
				final String dataset, final String table, final String[] actions)
				throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				bos.println(MartRunnerProtocol.SET_ACTIONS);
				bos.println(jobId);
				bos.println(partition + "," + dataset + "," + table);
				for (int i = 0; i < actions.length; i++) {
					bos.println(actions[i]);
					bos.println(MartRunnerProtocol.NEXT);
				}
				bos.println(MartRunnerProtocol.END_MESSAGE);
				bis.readLine(); // OK
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
		 * Retrieve job plan nodes for a given section.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @param jobId
		 *            the job ID.
		 * @param jobSection
		 *            the section to get nodes for.
		 * @return the job plan.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static Collection getActions(final String host,
				final String port, final String jobId,
				final JobPlanSection jobSection) throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				bos.println(MartRunnerProtocol.GET_ACTIONS);
				bos.println(jobId);
				bos.println(jobSection.getIdentifier());
				return (Collection) new ObjectInputStream(clientSocket
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
		 * Flag that a job email address has changed.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @param jobId
		 *            the job ID.
		 * @param email
		 *            the new email address.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void setEmailAddress(final String host,
				final String port, final String jobId, final String email)
				throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				bos.println(MartRunnerProtocol.EMAIL_ADDRESS);
				bos.println(jobId);
				bos.println(email == null ? "" : email);
				bis.readLine(); // OK
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
		 * Flag that a job thread count has changed.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @param jobId
		 *            the job ID.
		 * @param threadCount
		 *            the new thread count.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void setThreadCount(final String host, final String port,
				final String jobId, final int threadCount)
				throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				bos.println(MartRunnerProtocol.THREAD_COUNT);
				bos.println(jobId);
				bos.println(threadCount);
				bis.readLine(); // OK
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
		 * Flag that a job is to be started.
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
		public static void startJob(final String host, final String port,
				final String jobId) throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				bos.println(MartRunnerProtocol.START_JOB);
				bos.println(jobId);
				bis.readLine(); // OK
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
		 * Flag that a job is to be stopped.
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
		public static void stopJob(final String host, final String port,
				final String jobId) throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				bos.println(MartRunnerProtocol.STOP_JOB);
				bos.println(jobId);
				bis.readLine(); // OK
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
		 * Flag identifiers to be queued.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @param jobId
		 *            the job ID.
		 * @param identifiers
		 *            the identifiers.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void queue(final String host, final String port,
				final String jobId, final Collection identifiers)
				throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				bos.println(MartRunnerProtocol.QUEUE);
				bos.println(jobId);
				for (final Iterator i = identifiers.iterator(); i.hasNext();)
					bos.println(i.next());
				bos.println(MartRunnerProtocol.END_MESSAGE);
				bis.readLine(); // OK
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
		 * Flag identifiers to be unqueued.
		 * 
		 * @param host
		 *            the remote host.
		 * @param port
		 *            the remote port.
		 * @param jobId
		 *            the job ID.
		 * @param identifiers
		 *            the identifiers.
		 * @throws ProtocolException
		 *             if something went wrong.
		 */
		public static void unqueue(final String host, final String port,
				final String jobId, final Collection identifiers)
				throws ProtocolException {
			Socket clientSocket = null;
			try {
				clientSocket = Client.getClientSocket(host, port);
				final PrintWriter bos = new PrintWriter(clientSocket
						.getOutputStream(), true);
				final BufferedReader bis = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				bos.println(MartRunnerProtocol.UNQUEUE);
				bos.println(jobId);
				for (final Iterator i = identifiers.iterator(); i.hasNext();)
					bos.println(i.next());
				bos.println(MartRunnerProtocol.END_MESSAGE);
				bis.readLine(); // OK
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
