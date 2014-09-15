/*
 * Copyright 2011-14 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.openmuc.framework.dataaccess;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;

import java.io.IOException;
import java.util.List;

/**
 * The <code>Channel</code> class is used to access a single data field of a communication device. A desired channel can
 * be obtained using the <code>DataAccessService</code>. A channel instance can be used to
 * <ul>
 * <li>Access the latest record. That is the latest data record that the framework either sampled or received by
 * listening or an application set using <code>setLatestRecord</code>.</li>
 * <li>Directly read/write data from/to the corresponding communication device.</li>
 * <li>Access historical data that was stored by a data logger such as SlotsDB.</li>
 * <li>Get configuration information about this channel such as its unit.</li>
 * </ul>
 * <p/>
 * Note that only the call of the read or write functions will actually result in a corresponding read or write request
 * being sent to the communication device.
 */
public interface Channel {

    /**
     * Returns the ID of this channel. The ID is usually a meaningful string. It is used to get Channel objects using
     * the <code>DataAccessService</code>.
     *
     * @return the ID of this channel.
     */
    public String getId();

    /**
     * Returns the address of this channel.
     *
     * @return the address of this channel. The empty string if not configured.
     */
    public String getChannelAddress();

    /**
     * Returns the description of this channel.
     *
     * @return the description of this channel. The empty string if not configured.
     */
    public String getDescription();

    /**
     * Returns the unit of this channel. The unit is used for informational purposes only. Neither the framework nor any
     * driver does value conversions based on the configured unit.
     *
     * @return the unit of this channel.
     */
    public String getUnit();

    /**
     * Returns the value type of this channel. The value type is for informational purposes only. A data base is
     * encouraged to store values using the configured value type if it supports that value type. There is no guarantee
     * that the driver and data base that provide the values for this channel actually use a <code>Value</code>
     * implementation that corresponds to the configured value type.
     * <p/>
     * But usually an application does not need to know the value type of the channel because it can use the value type
     * of its choice by using the corresponding function of the <code>Value</code> interface. Conversion is done
     * transparently.
     *
     * @return the value type of this channel.
     */
    public ValueType getValueType();

    /**
     * Returns the channel's configured sampling interval in milliseconds.
     *
     * @return the channel's configured sampling interval in milliseconds. Returns -1 if not configured.
     */
    public int getSamplingInterval();

    /**
     * Returns the channel's configured sampling time offset in milliseconds.
     *
     * @return the channel's configured sampling time offset in milliseconds. Returns the default of 0 if not
     * configured.
     */
    public int getSamplingTimeOffset();

    /**
     * Returns the channel's configured logging interval in milliseconds.
     *
     * @return the channel's configured logging interval in milliseconds. Returns -1 if not configured.
     */
    public int getLoggingInterval();

    /**
     * Returns the channel's configured logging time offset in milliseconds.
     *
     * @return the channel's configured logging time offset in milliseconds. Returns the default of 0 if not configured.
     */
    public int getLoggingTimeOffset();

    /**
     * Returns the unique name of the communication driver that is used by this channel to read/write data.
     *
     * @return the unique name of the communication driver that is used by this channel to read/write data.
     */
    public String getDriverName();

    /**
     * Returns the channel's interface address.
     *
     * @return the channel's interface address. May be <code>null</code> if not configured.
     */
    public String getInterfaceAddress();

    /**
     * Returns the channel's device address.
     *
     * @return the channel's device address.
     */
    public String getDeviceAddress();

    /**
     * Returns the name of the communication device that this channel belongs to.
     *
     * @return the name of the communication device that this channel belongs to. The empty string if not configured.
     */
    public String getDeviceName();

    /**
     * Returns the description of the communication device that this channel belongs to.
     *
     * @return the description of the communication device that this channel belongs to. The empty string if not
     * configured.
     */
    public String getDeviceDescription();

    /**
     * Returns the current channel state.
     *
     * @return the current channel state.
     */
    public ChannelState getChannelState();

    /**
     * Returns the current state of the communication device that this channel belongs to.
     *
     * @return the current state of the communication device that this channel belongs to.
     */
    public DeviceState getDeviceState();

    /**
     * Adds a listener that is notified of new records received by sampling or listening.
     *
     * @param listener the record listener that is notified of new records.
     */
    public void addListener(RecordListener listener);

    /**
     * Removes a record listener.
     *
     * @param listener the listener shall be removed.
     */
    public void removeListener(RecordListener listener);

    /**
     * Returns <code>true</code> if a connection to the channel's communication device exist.
     *
     * @return <code>true</code> if a connection to the channel's communication device exist.
     */
    public boolean isConnected();

    /**
     * Returns the latest record of this channel. Every channel holds its latest record in memory. There exist three
     * possible source for the latest record:
     * <ul>
     * <li>It may be provided by a communication driver that was configured to sample or listen on the channel. In this
     * case the timestamp of the record represents the moment in time that the value was received by the driver.</li>
     * <li>An application may also set the latest record using <code>setLatestRecord</code>.</li>
     * <li>Finally values written using <code>write</code> are also stored as the latest record</li>
     * </ul>
     *
     * @return the latest record.
     */
    public Record getLatestRecord();

    /**
     * Sets the latest record of this channel. This function should only be used with channels that are neither sampling
     * nor listening. Using this function it is possible to realize "virtual" channels that get their data not from
     * drivers but from applications in the framework.
     * <p/>
     * Note that the framework treats the passed record in exactly the same way as if it had been received from a
     * driver. In particular that means:
     * <ul>
     * <li>If data logging is enabled for this channel the latest record is being logged by the registered loggers.</li>
     * <li>Other applications can access the value set by this function using <code>getLatestRecord</code>.</li>
     * <li>Applications are notified of the new record if they registered as listeners using <code>addListener</code>.
     * <li>If a scaling factor has been configured for this channel then the value passed to this function is scaled.</li>
     * </ul>
     *
     * @param record the record to be set.
     */
    public void setLatestRecord(Record record);

    /**
     * Writes the given value to the channel's corresponding data field in the connected communication device. If an
     * error occurs, the returned <code>Flag</code> will indicate this.
     *
     * @param value the value that is to be written
     * @return the flag indicating whether the value was successfully written ( <code>Flag.VALID</code>) or not (any
     * other flag).
     */
    public Flag write(Value value);

    /**
     * Schedules a List<Records> with future timestamps as write tasks <br>
     * This function will schedule single write tasks to the provided timestamps.<br>
     * Once this function is called, previously scheduled write tasks will be erased.<br>
     *
     * @param records each record contains the value that is to be written and the timestamp indicating when it should be
     *                written. The flag of the record is ignored.
     */
    public void write(List<Record> records);

    /**
     * Returns a <code>WriteValueContainer</code> that corresponds to this channel. This container can be passed to the
     * write function of <code>DataAccessService</code> to write several values in one transaction.
     *
     * @return a <code>WriteValueContainer</code> that corresponds to this channel.
     */
    public WriteValueContainer getWriteContainer();

    /**
     * Actively reads a value from the channel's corresponding data field in the connected communication device. If an
     * error occurs it will be indicated in the returned record's flag.
     *
     * @return the record containing the value read, the time the value was received and a flag indicating success (
     * <code>Flag.VALID</code>) or a an error (any other flag).
     */
    public Record read();

    /**
     * Returns a <code>ReadRecordContainer</code> that corresponds to this channel. This container can be passed to the
     * <code>read</code> function of <code>DataAccessService</code> to read several values in one transaction.
     *
     * @return a <code>ReadRecordContainer</code> that corresponds to this channel.
     */
    public ReadRecordContainer getReadContainer();

    /**
     * Returns the logged data record whose timestamp equals the given <code>time</code>. Note that it is the data
     * logger's choice whether it stores values using the timestamp that the driver recorded when it received it or the
     * timestamp at which the value is to be logged. If the former is the case then this function is not useful because
     * it is impossible for an application to know the exact time at which a value was received. In this case use
     * <code>getLoggedRecords</code> instead.
     *
     * @param time the time in milliseconds since midnight, January 1, 1970 UTC.
     * @return the record that has been stored by the framework's data logger at the given <code>timestamp</code>.
     * Returns <code>null</code> if no record exists for this point in time.
     * @throws DataLoggerNotAvailableException if no data logger is installed and therefore no logged data can be accessed.
     * @throws IOException                     if any kind of error occurs accessing the logged data.
     */
    public Record getLoggedRecord(long time) throws DataLoggerNotAvailableException, IOException;

    /**
     * Returns a list of all logged data records with timestamps from <code>startTime</code> up until now.
     *
     * @param startTime the starting time in milliseconds since midnight, January 1, 1970 UTC. inclusive
     * @return a list of all logged data records with timestamps from <code>startTime</code> up until now.
     * @throws DataLoggerNotAvailableException if no data logger is installed and therefore no logged data can be accessed.
     * @throws IOException                     if any kind of error occurs accessing the logged data.
     */
    public List<Record> getLoggedRecords(long startTime)
            throws DataLoggerNotAvailableException, IOException;

    /**
     * Returns a list of all logged data records with timestamps from <code>startTime</code> to <code>endTime</code>
     * inclusive.
     *
     * @param startTime the starting time in milliseconds since midnight, January 1, 1970 UTC. inclusive
     * @param endTime   the ending time in milliseconds since midnight, January 1, 1970 UTC. inclusive
     * @return a list of all logged data records with timestamps from <code>startTime</code> to <code>endTime</code>
     * inclusive.
     * @throws DataLoggerNotAvailableException if no data logger is installed and therefore no logged data can be accessed.
     * @throws IOException                     if any kind of error occurs accessing the logged data.
     */
    public List<Record> getLoggedRecords(long startTime, long endTime)
            throws DataLoggerNotAvailableException,
            IOException;

}