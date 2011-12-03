package org.onebusaway.nyc.transit_data_manager.siri;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

public class SiriServiceDao implements SiriServicePersister {

  static final Logger _log = LoggerFactory.getLogger(SiriServiceDao.class);

  private HibernateTemplate _template;

  @Autowired
  public void setSessionFactory(SessionFactory sessionFactory) {
    _template = new HibernateTemplate(sessionFactory);
  }

  public HibernateTemplate getHibernateTemplate() {
    return _template;
  }

  @Transactional(rollbackFor = Throwable.class)
  @Override
  public void saveOrUpdateServiceAlert(ServiceAlertBean serviceAlertBean) {
    ServiceAlertRecord record = getServiceAlertByServiceAlertId(serviceAlertBean.getId());
    if (record != null) {
      record.setDeleted(false);
      _template.saveOrUpdate(record.updateFrom(serviceAlertBean));
    } else {
      _template.saveOrUpdate(new ServiceAlertRecord(serviceAlertBean));
    }
  }

  @Override
  public ServiceAlertBean deleteServiceAlertById(String serviceAlertId) {
    ServiceAlertRecord record = getServiceAlertByServiceAlertId(serviceAlertId);
    if (record == null)
      return null;
    record.setDeleted(true);
    _template.saveOrUpdate(record);
    return ServiceAlertRecord.toBean(record);
  }

  private ServiceAlertRecord getServiceAlertByServiceAlertId(
      String serviceAlertId) {
    List<ServiceAlertRecord> list = _template.find(
        "from ServiceAlertRecord where service_alert_id=?", serviceAlertId);
    if (list.size() > 0)
      return list.get(0);
    else
      return null;
  }

  @Override
  public List<ServiceAlertBean> getAllActiveServiceAlerts() {
    return getServiceAlerts("from ServiceAlertRecord r where r.deleted = false");
  }

  @Override
  public List<ServiceAlertBean> getAllServiceAlerts() {
    return getServiceAlerts("from ServiceAlertRecord");
  }

  private List<ServiceAlertBean> getServiceAlerts(String hsql) {
    List<ServiceAlertBean> results = new ArrayList<ServiceAlertBean>();
    String hql = hsql;
    List<Object> list = _template.find(hql);
    _log.info("Ran query: " + hql + " size of results " + list.size());
    for (Object o : list) {
      ServiceAlertBean b = ServiceAlertRecord.toBean((ServiceAlertRecord) o);
      results.add(b);
    }
    return results;
  }

  @Override
  public void saveOrUpdateSubscription(ServiceAlertSubscription subscription) {
    _template.saveOrUpdate(subscription);
  }

  @Override
  public void deleteSubscription(ServiceAlertSubscription subscription) {
    _template.delete(subscription);
  }

  @Override
  public List<ServiceAlertSubscription> getAllActiveSubscriptions() {
    return (List<ServiceAlertSubscription>) _template.find("from ServiceAlertSubscription");
  }

}