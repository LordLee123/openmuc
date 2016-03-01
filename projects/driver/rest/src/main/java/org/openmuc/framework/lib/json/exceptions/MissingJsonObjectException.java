/*
 * Copyright 2011-16 Fraunhofer ISE
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
package org.openmuc.framework.lib.json.exceptions;

public class MissingJsonObjectException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3245778161912001429L;
	private String message = "Searched JsonObject is missing. ";

	public MissingJsonObjectException() {
	}

	public MissingJsonObjectException(String message) {
		this.message = message;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
