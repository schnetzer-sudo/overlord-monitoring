SELECT CASE WHEN p.ProjectID LIKE '100\_VTG\_%' THEN '100_VTG_<PARTNER>'
            WHEN p.ProjectID = '110_VTG_SalesInvoice' THEN '110_VTG_SalesInvoice'
            ELSE 'uebrige 110_*' END AS form,
       COUNT(DISTINCT p.ProjectID) AS projekte, COUNT(*) AS prozesse,
       ROUND(100.0 * COUNT(*) / (SELECT COUNT(*) FROM Process p2
             JOIN ProjectMandant pm2 ON pm2.ProjectID = p2.ProjectID
             WHERE pm2.MandantID='VOTG'), 2) AS anteil
FROM Process p JOIN ProjectMandant pm ON pm.ProjectID = p.ProjectID
WHERE pm.MandantID = 'VOTG' GROUP BY form ORDER BY prozesse DESC;
