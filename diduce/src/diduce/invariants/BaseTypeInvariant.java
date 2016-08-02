/*
DIDUCE: A tool for detecting and root-causing bugs in Java programs.
Copyright (C) 2002 Sudheendra Hangal

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

For more details, please see:
http://www.gnu.org/copyleft/gpl.html

*/

package diduce.invariants;
public class BaseTypeInvariant extends TypeTrackingInvariant {

public void newSample (Object ref, int new_val, int old_val, String spp_desc, int spp_num)
{
    newSample (ref, spp_desc, get_description());
}

public void newSample (Object ref, long new_val, long old_val, String spp_desc, int spp_num)
{
    newSample (ref, spp_desc, get_description());
}

public void newSample (Object ref, Object new_val, Object old_val, String spp_desc, int spp_num)
{
    newSample (ref, spp_desc, get_description());
}

public String get_description () { return "Base Object Type"; }

}

