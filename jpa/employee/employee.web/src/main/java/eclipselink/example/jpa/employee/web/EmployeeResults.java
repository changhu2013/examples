/*******************************************************************************
 * Copyright (c) 2010-2013 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *  dclarke - initial
 ******************************************************************************/
package eclipselink.example.jpa.employee.web;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;

import eclipselink.example.jpa.employee.model.Employee;
import eclipselink.example.jpa.employee.services.Diagnostics.SQLTrace;
import eclipselink.example.jpa.employee.services.EmployeeCriteria;
import eclipselink.example.jpa.employee.services.EmployeeRepository;
import eclipselink.example.jpa.employee.services.paging.EntityPaging;

/**
 * Backing bean to manage search results for an Employee query. The results can
 * either be paged or displayed in a single scrolling list.
 * 
 * @author dclarke
 * @since EclipseLink 2.4.2
 */
@ManagedBean
@ViewScoped
public class EmployeeResults {

    protected static final String PAGE = "/employee/results?faces-redirect=true";

    private EmployeeRepository repository;

    /**
     * Current employees being shown
     */
    private List<Employee> employees;

    private EntityPaging<Employee> paging;

    private int currentPage = 1;

    private EmployeeCriteria criteria;

    public EmployeeRepository getRepository() {
        return repository;
    }

    @EJB
    public void setRepository(EmployeeRepository repository) {
        this.repository = repository;
    }

    public EntityPaging<Employee> getPaging() {
        return this.paging;
    }

    @PostConstruct
    public void initialize() {
        Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
        criteria = (EmployeeCriteria) flash.get(SearchEmployees.CRITERIA);

        this.currentPage = 1;
        this.employees = null;

        this.paging = getRepository().getPaging(criteria);

        if (!hasPaging()) {
            startSqlCapture();

            stopSqlCapture();
        }
    }

    public List<Employee> getEmployees() {
        startSqlCapture();
        if (this.employees == null) {
            if (hasPaging()) {
                this.employees = getPaging().get(this.currentPage);
            } else {
                this.employees = getRepository().getEmployees(criteria);
            }
        }
        stopSqlCapture();
        return this.employees;
    }

    public boolean hasPaging() {
        return this.paging != null;
    }

    public int getSize() {
        if (hasPaging()) {
            return this.paging.size();
        }
        return getEmployees().size();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getNumPages() {
        if (hasPaging()) {
            return this.paging.getNumPages();
        }
        return 1;
    }

    public String next() {
        if (getHasNext()) {
            this.currentPage++;
            this.employees = null;
        }
        return null;
    }

    public boolean getHasNext() {
        return this.currentPage < getNumPages();
    }

    public String previous() {
        if (getHasPrevious()) {
            this.currentPage--;
            this.employees = null;
        }
        return null;
    }

    public boolean getHasPrevious() {
        return this.currentPage > 1;
    }

    public String edit(Employee employee) {
        Flash flashScope = FacesContext.getCurrentInstance().getExternalContext().getFlash();
        flashScope.put("employee", employee);

        return EditEmployee.PAGE;
    }

    protected void startSqlCapture() {
        addMessages(getRepository().getDiagnostics().start());
    }

    protected void stopSqlCapture() {
        addMessages(getRepository().getDiagnostics().stop());
    }

    /**
     * Add each SQL string to the messages TODO: Allow this to be
     * enabled/disabled
     */
    private void addMessages(SQLTrace sqlTrace) {
        for (String entry : sqlTrace.getEntries()) {
            FacesContext.getCurrentInstance().addMessage("SQL", new FacesMessage(entry));
        }
    }

}
