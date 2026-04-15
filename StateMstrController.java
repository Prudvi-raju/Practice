// Created By Anjali Singh , DELHI
package com.bisagn.demo.controller.rbac;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import com.bisagn.demo.model.rbac.OM_TB_STATE_M;
import com.bisagn.demo.services.rbac.StateDAO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping(value = { "admin" })
public class StateMstrController {

	@Autowired
	private StateDAO statedao;
	@Autowired
	private SessionFactory sessionFactory;

	@GetMapping("/state_mstUrl")
	public ModelAndView stateUrl(ModelMap map) {

		map.put("state_mstCMD", new OM_TB_STATE_M());

		// for dynamic listing for STATE/UT flag
		map.put("stateUtList", statedao.getStateUTList());

		return new ModelAndView("rbac/state_mst");
	}

	@PostMapping("/state_mstAction")
	@ResponseBody
	public String state_mstAction(@ModelAttribute("state_mstCMD") OM_TB_STATE_M state, HttpServletRequest request,
			HttpSession session) {

		Session sessionHQL = sessionFactory.openSession();
		Transaction tx = null;

		try {

			String username = (String) session.getAttribute("username");

			// we are adding this for the unit_code and modified_code
			String session_username = session.getAttribute("username") != null
					? session.getAttribute("username").toString()
					: "SYSTEM";

			Date currentTimestamp = new Date();

			Integer userUnitCode = null;
			try {
			    Query<Integer> checkQuery_1 = sessionHQL.createQuery(
			        "select g.createdUnit from SC_TB_GUEST_LOGIN g where g.empSeqNo = :empSeq_no", Integer.class);

			    checkQuery_1.setParameter("empSeq_no", session_username);
			    checkQuery_1.setMaxResults(1); 

			    userUnitCode = checkQuery_1.uniqueResult();

			} catch (Exception e) {
			    e.printStackTrace(); 
			}

			if (username == null || username.trim().isEmpty()) {
				return "Session expired. Please login again.";
			}

			String stateCode = request.getParameter("stateCode");
			String oldStateCode = request.getParameter("old_state_code");
			String stateName = request.getParameter("stateName");
			String stateUtFlag = request.getParameter("stateUtFlag");
			String chargeableFlag = request.getParameter("chargeableFlag");
			String status = request.getParameter("softdeleteFlag");
			String abbreviation = request.getParameter("abbreviation");
			String hardSoftDateStr = request.getParameter("hardSoftDate");
			String chargeDateStr = request.getParameter("chargeDate");
			String oldStateAbbr = request.getParameter("old_state_abbr");

			if (stateCode == null || stateCode.trim().isEmpty())
				return "Please Enter State Code";

			if (stateName == null || stateName.trim().isEmpty())
				return "Please Enter State Name";

			if (abbreviation == null || abbreviation.trim().isEmpty())
				return "Please Enter Abbreviation";

			if (stateUtFlag == null || stateUtFlag.trim().isEmpty())
				return "Please Select State / UT";

			if (chargeableFlag == null || chargeableFlag.trim().isEmpty())
				return "Please Select Chargeable Flag";

			if (hardSoftDateStr == null || hardSoftDateStr.trim().isEmpty()) {
				return "Please Enter HardSoft Effective Date";
			}

			if (chargeDateStr == null || chargeDateStr.trim().isEmpty()) {
				return "Please Enter Chargeable Effective Date";
			}

			if (status == null || status.equals("0"))
				return "Please Select Status";

			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

			Date hardSoftDate = null;
			Date chargeDate = null;

			try {
				if (hardSoftDateStr != null && !hardSoftDateStr.isEmpty()) {
					hardSoftDate = sdf.parse(hardSoftDateStr);
				}
				if (chargeDateStr != null && !chargeDateStr.isEmpty()) {
					chargeDate = sdf.parse(chargeDateStr);
				}
			} catch (Exception e) {
				return "Invalid Date Format";
			}

			stateCode = stateCode.trim();
			stateName = stateName.trim();

			tx = sessionHQL.beginTransaction();
              
			if (oldStateCode == null || oldStateCode.trim().isEmpty()) {
                //Insert Logic
				Long count = (Long) sessionHQL.createQuery(
						"SELECT COUNT(*) FROM OM_TB_STATE_M WHERE UPPER(stateCode)=:code AND UPPER(abbreviation)=:abbr")
						.setParameter("code", stateCode.toUpperCase()).setParameter("abbr", abbreviation.toUpperCase())
						.uniqueResult();

				if (count > 0)
					return "State Code or Abbrivation Already Exists";

				state.setStateCode(stateCode);
				state.setStateName(stateName);
				state.setAbbreviation(abbreviation.trim());
				state.setStateUtFlag(stateUtFlag);
				state.setChargeableFlag(chargeableFlag);
				state.setHardSoftFlag("H");

				state.setSoftdeleteFlag(status);

				state.setCreatedBy(session_username);
				state.setCreatedUnit(userUnitCode != null ? Long.valueOf(userUnitCode) : 0L);
				state.setCreatedDate(currentTimestamp);

				state.setModifiedBy(session_username);
				state.setModifiedUnit(userUnitCode != null ? Long.valueOf(userUnitCode) : 0L);
				state.setModifiedDate(currentTimestamp);

				state.setHardSoftEffDate(hardSoftDate);
				state.setChrgEffDate(chargeDate);

				sessionHQL.save(state);

				tx.commit();

				return "State Saved Successfully";
			}

			else {
				//Update logic
				String newStateCodeUpper = stateCode.toUpperCase();
				String newAbbrUpper = abbreviation.toUpperCase();

				String oldSateCodeUpper = oldStateCode.toUpperCase();
				String oldAbbrUpper = oldStateAbbr.toUpperCase(); // make sure you have oldType

				boolean isTypeCodeChanged = !newStateCodeUpper.equals(oldSateCodeUpper);
				boolean isTypeChanged = !newAbbrUpper.equals(oldAbbrUpper);
				if (isTypeCodeChanged || isTypeChanged) {
					/*
					 * String countQuery =
					 * "SELECT COUNT(*) FROM OM_TB_STATE_M WHERE UPPER(stateCode) = :code AND  UPPER(abbreviation)=:abbr"
					 * ;
					 * 
					 * Long count = sessionHQL.createQuery(countQuery, Long.class)
					 * .setParameter("code", stateCode.toUpperCase()) .setParameter("abbr",
					 * abbreviation.toUpperCase()) .uniqueResult(); System.out.print(count); if
					 * (count != null && count > 0) { return "State Name Already Exists"; }
					 */
                   
					String sql1 = "SELECT COUNT(*) FROM OM_TB_STATE_M WHERE UPPER(state_code) = :code OR  UPPER(abbreviation)=:abbr";

					NativeQuery<?> qCheck1 = sessionHQL.createNativeQuery(sql1);

					qCheck1.setParameter("code", stateCode.toUpperCase());
					qCheck1.setParameter("abbr", abbreviation.toUpperCase());

					Number result = (Number) qCheck1.uniqueResult();
					Long count = (result != null) ? result.longValue() : 0L;

					System.out.println("Count is: " + count);

					if (count > 0) {
						return "State Name Already Exists";
					}

				}
    
				String hql = "UPDATE OM_TB_STATE_M SET " + "stateCode=:newCode, " + "stateName=:name, "
						+ "abbreviation=:abbr, " + "stateUtFlag=:ut, " + "chargeableFlag=:charge, "
						+ "softdeleteFlag=:status, " + "hardSoftEffDate=:hsDate, " + "chrgEffDate=:chDate, "
						+ "modifiedBy=:user, " + "modifiedUnit=:unit, " + "modifiedDate=:date "
						+ "WHERE stateCode=:oldCode";

				int rows = sessionHQL.createQuery(hql).setParameter("newCode", stateCode)
						.setParameter("oldCode", oldStateCode).setParameter("name", stateName)
						.setParameter("abbr", abbreviation).setParameter("ut", stateUtFlag)
						.setParameter("charge", chargeableFlag).setParameter("hsDate", hardSoftDate)
						.setParameter("chDate", chargeDate).setParameter("status", status)
						.setParameter("user", session_username)
						.setParameter("unit", userUnitCode != null ? Long.valueOf(userUnitCode) : 0L)
						.setParameter("date", currentTimestamp).executeUpdate();

				tx.commit();

				if (rows > 0)
					return "State Updated Successfully";
				else
					return "State Not Found";
			}

		} catch (Exception e) {

			if (tx != null)
				tx.rollback();

			e.printStackTrace();
			return "Error : " + e.getMessage();

		} finally {
			sessionHQL.close();
		}
	}
    //url for delete
	@PostMapping("/delete_state")
	@ResponseBody
	public String deleteState(HttpServletRequest request, HttpSession session) {

		Session sessionHQL = sessionFactory.openSession();
		Transaction tx = null;

		try {

			String username = (String) session.getAttribute("username");

			if (username == null || username.trim().isEmpty())
				return "Session expired. Please login again.";

			String id = request.getParameter("id");

			if (id == null || id.trim().isEmpty())
				return "Invalid State Code";

			tx = sessionHQL.beginTransaction();

			String hql = "UPDATE OM_TB_STATE_M SET " + "softdeleteFlag='D', " + "softdeleteDate=:date, "
					+ "modifiedBy=:user, " + "modifiedUnit=:unit, " + "modifiedDate=:modDate "
					+ "WHERE stateCode=:code";

			Integer userUnitCode = null;

			int rows = sessionHQL.createQuery(hql).setParameter("date", new Date()).setParameter("user", username)
					.setParameter("unit", userUnitCode != null ? Long.valueOf(userUnitCode) : 0L)
					.setParameter("modDate", new Date()).setParameter("code", id.trim()).executeUpdate();

			tx.commit();

			return rows > 0 ? "State Deleted Successfully" : "State Not Found";

		} catch (Exception e) {
			if (tx != null)
				tx.rollback();
			e.printStackTrace();
			return "Error : " + e.getMessage();
		} finally {
			sessionHQL.close();
		}
	}
    //Used for the dynamic listing of the table 
	@PostMapping("/getStateReport")
	@ResponseBody
	public List<Map<String, Object>> getStateReport(int startPage, int pageLength, String Search, int orderColunm,
			String orderType,
			@RequestParam(required = false) String stateCode,
		    @RequestParam(required = false) String stateName,
		    @RequestParam(required = false) String abbreviation,
		    @RequestParam(required = false) String stateUtFlag,
		    @RequestParam(required = false) String chargeableFlag,
		    @RequestParam(required = false) String softdeleteFlag,
		    @RequestParam(required = false) String hardSoftDate,
		    @RequestParam(required = false) String chargeDate, HttpSession session) {

		return statedao.stateReport(startPage, pageLength, Search, orderColunm, orderType, stateCode, stateName, abbreviation,
                stateUtFlag, chargeableFlag, 
                softdeleteFlag,hardSoftDate, chargeDate, session);
	}
	//Used for the table count
	@PostMapping("/getStateReportCount")
	@ResponseBody
	public long getStateReportCount(int startPage, int pageLength, String Search, int orderColunm, String orderType,
			    @RequestParam(required = false) String stateCode,
			    @RequestParam(required = false) String stateName,
			    @RequestParam(required = false) String abbreviation,
			    @RequestParam(required = false) String stateUtFlag,
			    @RequestParam(required = false) String chargeableFlag,
			    @RequestParam(required = false) String softdeleteFlag,
			    @RequestParam(required = false) String hardSoftDate,
			    @RequestParam(required = false) String chargeDate,
			HttpSession session) {

		return statedao.stateReportCount(startPage, pageLength, Search, orderColunm, orderType, stateCode, stateName, abbreviation,
                stateUtFlag, chargeableFlag, 
                softdeleteFlag,hardSoftDate, chargeDate, session);
	}
    
	@GetMapping("/checkStateCode")
	@ResponseBody
	public boolean checkDuplicate(@RequestParam String value,
	                             @RequestParam String field,
	                             @RequestParam(required = false) String oldCode,
	                             @RequestParam(required = false) String oldAbbr) {

	    Session session = sessionFactory.openSession();

	    Query<Long> query = null;

	    if ("stateCode".equals(field)) {

	        String hql = "SELECT COUNT(*) FROM OM_TB_STATE_M " +
	                     "WHERE UPPER(stateCode)=:val " +
	                     "AND (:oldCode IS NULL OR UPPER(stateCode) != :oldCode)";

	        query = session.createQuery(hql, Long.class);
	        query.setParameter("val", value.toUpperCase());
	        query.setParameter("oldCode",
	                (oldCode != null && !oldCode.isEmpty()) ? oldCode.toUpperCase() : null);

	    } else if ("abbreviation".equals(field)) {

	       
	        String hql = "SELECT COUNT(*) FROM OM_TB_STATE_M " +
	                     "WHERE UPPER(abbreviation)=:val " +
	                     "AND (:oldAbbr IS NULL OR UPPER(abbreviation) != :oldAbbr)";

	        query = session.createQuery(hql, Long.class);
	        query.setParameter("val", value.toUpperCase());
	        query.setParameter("oldAbbr",
	                (oldAbbr != null && !oldAbbr.isEmpty()) ? oldAbbr.toUpperCase() : null);
	    }

	    Long count = (query != null) ? query.uniqueResult() : 0L; 

	    session.close();

	    return count > 0;
	}
	
}