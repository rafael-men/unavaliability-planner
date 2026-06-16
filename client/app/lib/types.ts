
export type Role =
  | 'admin_master'
  | 'admin_editor'
  | 'admin_leitor'
  | 'lider'
  | 'socio'
  | 'colaborador';

export type UserStatus = 'pending' | 'approved' | 'rejected';

export type UnavailabilityStatus = 'pending' | 'approved' | 'rejected';

export type UnavailabilityType = 'prolongado' | 'pontual';


export interface User {
  id: number;
  email: string;
  full_name: string;
  nome?: string;
  role: Role;
  status?: UserStatus;
  department?: string | null;
  member_id?: number | null;
  created_at?: string | null;
}

export interface Cliente {
  id: number;
  nome: string;
  descricao: string | null;
  ativo: boolean;
}

export interface UserClienteLink {
  user_id: number;
  cliente_id: number;
  ativo: boolean;
}

export interface ClienteRef {
  id: number;
  nome: string;
}

export interface Evento {
  id: number;
  nome: string;
  descricao: string | null;
  data_inicio: string;
  data_fim: string;
  cliente_ids?: number[];
  clientes?: ClienteRef[];
}

export interface Member {
  id: number;
  name: string;
  email: string;
  area?: string | null;
  squad?: string | null;
  funcao?: string | null;
  report_to?: string | null;
  operacoes?: boolean | null;
  day_offs_quota?: number | null;
}

export interface EventConflict {
  id: number;
  nome: string;
  descricao: string | null;
  data_inicio: string;
  data_fim: string;
  clientes: string[];
}

export interface UnavailabilityRecord {
  id: number;
  user_id: number;
  full_name?: string | null;
  user_name?: string | null;
  user_email?: string | null;
  user_role?: Role | null;
  unavailability_type: UnavailabilityType;
  department?: string | null;
  start_date: string;
  end_date: string;
  total_days: number;
  status: UnavailabilityStatus;
  reviewed_by?: number | null;
  reviewed_at?: string | null;
  created_at?: string | null;
  event_conflicts?: EventConflict[];
}

export interface NotificationItem {
  id: string;
  type: 'pending_user' | 'pending_unavailability';
  title: string;
  subtitle: string;
  created_at: string | null;
  href: string;
}
